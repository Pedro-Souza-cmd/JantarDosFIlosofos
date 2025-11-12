package Jantar;

import java.util.concurrent.Semaphore;

public class Mesa {
	static final int PENSANDO = 1;
	static final int COMENDO = 2;
	static final int FOME = 3;
	static final int NR_FILOSOFOS = 5;
	static final int MAX_TRACE = 50;

	boolean[] garfos = new boolean[NR_FILOSOFOS];
	int[] filosofos = new int[NR_FILOSOFOS];
	int[] tentativas = new int[NR_FILOSOFOS]; // esperas na rodada atual (opcional)
	int[] contador = new int[NR_FILOSOFOS]; // refeições por filósofo
	int[] tentativasTotal = new int[NR_FILOSOFOS]; // esperas acumuladas por filósofo

	private int totalRefeicoes = 0;
	private int contadorExecucoesDetalhadas = 0;
	private boolean avisouFimTrace = false;

	private final Semaphore mordomo = new Semaphore(1); // apenas um filósofo por vez

	public Mesa() {
		for (int i = 0; i < NR_FILOSOFOS; ++i) {
			garfos[i] = true;
			filosofos[i] = PENSANDO;
			tentativas[i] = 0;
			tentativasTotal[i] = 0;
			contador[i] = 0;
		}
	}

	// >>> NOVO: conta espera pelo semáforo
	public void entrarNoRefeitorio(int filosofo) throws InterruptedException {
		if (!mordomo.tryAcquire()) {
			// não havia permissão disponível: conta como 1 espera
			tentativas[filosofo]++;
			tentativasTotal[filosofo]++;
			mordomo.acquire(); // agora bloqueia até liberar
		}
		// se conseguiu no tryAcquire, entrou sem esperar (não soma)
	}

	public void sairDoRefeitorio() {
		mordomo.release();
	}

	public synchronized void pegarGarfos(int filosofo) {
		filosofos[filosofo] = FOME;
		int esq = garfoEsquerdo(filosofo);
		int dir = garfoDireito(filosofo);

		// Mantém a contagem de espera também aqui 
		while (!garfos[esq] || !garfos[dir]) {
			try {
				tentativas[filosofo]++;
				tentativasTotal[filosofo]++;
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}

		garfos[esq] = false;
		garfos[dir] = false;
		filosofos[filosofo] = COMENDO;

		contador[filosofo]++;
		totalRefeicoes++;

		if (contadorExecucoesDetalhadas < MAX_TRACE) {
			contadorExecucoesDetalhadas++;
			System.out.println("\n========== Execução #" + contadorExecucoesDetalhadas + " ==========");
			System.out.printf("Filósofo %-2d parou de pensar e começou a comer (usando garfos %-2d e %-2d)%n",
					(filosofo + 1), esq, dir);
			imprimeEstadosFilosofos();
			imprimeGarfos();
			System.out.println();
		} else if (!avisouFimTrace && contadorExecucoesDetalhadas == MAX_TRACE) {
			avisouFimTrace = true;
			System.out.println("\n========== Limite de " + MAX_TRACE
					+ " execuções detalhadas atingido. Trace pausado. ==========\n");
		}

		// zera “tentativas” da rodada, se você quiser acompanhar só o “último ciclo”
		tentativas[filosofo] = 0;
	}

	public synchronized void returningGarfos(int filosofo) {
		if (contadorExecucoesDetalhadas < MAX_TRACE) {
			System.out.printf("Filósofo %-2d terminou de comer e voltou a pensar%n", (filosofo + 1));
		}
		filosofos[filosofo] = PENSANDO;
		garfos[garfoEsquerdo(filosofo)] = true;
		garfos[garfoDireito(filosofo)] = true;
		notifyAll();
	}

	public int garfoEsquerdo(int filosofo) {
		return filosofo;
	}

	public int garfoDireito(int filosofo) {
		return (filosofo + 1) % NR_FILOSOFOS;
	}

	// === Impressão alinhada ===
	public void imprimeEstadosFilosofos() {
		System.out.print("Filósofos  = [ ");
		for (int i = 0; i < NR_FILOSOFOS; ++i) {
			String estado = switch (filosofos[i]) {
			case PENSANDO -> "PENSANDO";
			case COMENDO -> "COMENDO ";
			case FOME -> "FOME     ";
			default -> "?";
			};
			System.out.printf("%d:%-9s ", (i + 1), estado);
		}
		System.out.println("]");
	}

	public void imprimeGarfos() {
		System.out.print("Garfos     = [ ");
		for (int i = 0; i < NR_FILOSOFOS; ++i) {
			String estado = garfos[i] ? "LIVRE" : "OCUPADO";
			System.out.printf("%d:%-8s ", i, estado);
		}
		System.out.println("]");
	}

	public void imprimirEstatisticasFinais() {
		// --- métricas básicas (mantém sua lógica corrigida) ---
		int totalOperacoes = totalRefeicoes; // operação = início de comer
		int totalEsperas = 0;
		for (int i = 0; i < NR_FILOSOFOS; i++)
			totalEsperas += tentativasTotal[i];

		double mediaRef = (double) totalRefeicoes / NR_FILOSOFOS;
		double mediaEsp = (double) totalEsperas / NR_FILOSOFOS;

		// fairness
		int max = contador[0], min = contador[0];
		int idxMax = 0, idxMin = 0;
		for (int i = 1; i < NR_FILOSOFOS; i++) {
			if (contador[i] > max) {
				max = contador[i];
				idxMax = i;
			}
			if (contador[i] < min) {
				min = contador[i];
				idxMin = i;
			}
		}
		int diff = max - min;
		double variacao = (max > 0) ? (diff * 100.0 / max) : 0.0;

		// starvation (heurística original)
		boolean houveStarvation = false;
		for (int i = 0; i < NR_FILOSOFOS; i++) {
			if (tentativasTotal[i] > contador[i] * 5) {
				houveStarvation = true;
				break;
			}
		}

		// estado final (texto no estilo antigo)
		StringBuilder estados = new StringBuilder("Filósofos = [ ");
		for (int i = 0; i < NR_FILOSOFOS; i++) {
			estados.append(estadoFix(i)).append(" ");
		}
		estados.append("]");

		StringBuilder garfosSb = new StringBuilder("Garfos    = [ ");
		for (int i = 0; i < NR_FILOSOFOS; i++) {
			garfosSb.append(garfos[i] ? "LIVRE   " : "OCUPADO ");
		}
		garfosSb.append("]");

		// ===== impressão com caixa alinhada =====
		boxTop();
		boxLineCenter("ESTATÍSTICAS FINAIS DO JANTAR DOS FILÓSOFOS");
		boxSep();

		boxLineBlank();
		boxLine("DESEMPENHO INDIVIDUAL:");
		boxLine("─────────────────────");
		for (int i = 0; i < NR_FILOSOFOS; i++) {
			// exibição 1..5
			boxLine(String.format("Filósofo %-2d: %3d refeições | %4d esperas", (i + 1), contador[i],
					tentativasTotal[i]));
		}
		boxLineBlank();

		boxSep();
		boxLine("ESTATÍSTICAS GERAIS:");
		boxLine("───────────────────");
		boxLine(String.format("Total de operações:            %5d", totalOperacoes));
		boxLine(String.format("Total de refeições:            %5d", totalRefeicoes));
		boxLine(String.format("Total de esperas:              %5d", totalEsperas));
		boxLine(String.format("Média de refeições/filósofo:  %5.1f", mediaRef));
		boxLine(String.format("Média de esperas/filósofo:    %5.1f", mediaEsp));
		boxLineBlank();

		boxSep();
		boxLine("ANÁLISE DE FAIRNESS (JUSTIÇA):");
		boxLine("──────────────────────────────");
		boxLine(String.format("Filósofo que mais comeu:      Filósofo %-2d (%d vezes)", (idxMax + 1), max));
		boxLine(String.format("Filósofo que menos comeu:     Filósofo %-2d (%d vezes)", (idxMin + 1), min));
		boxLine(String.format("Diferença (max - min):        %d refeições", diff));
		boxLine(String.format("Variação percentual:          %.1f%%", variacao));
		boxLineBlank();

		boxLine(houveStarvation ? "⚠  Possível starvation detectado!"
				: "✓  Sistema balanceado - sem starvation detectado");
		boxLineBlank();

		boxSep();
		boxLine("ESTADO FINAL:");
		boxLine("────────────");
		boxLineFit(estados.toString());
		boxLineFit(garfosSb.toString());
		boxLineBlank();

		boxSep();
		boxLine("ROTAÇÃO DOS GARFOS (Histórico de Uso):");
		boxLine("──────────────────────────────────────");
		for (int i = 0; i < NR_FILOSOFOS; i++) {
			int esq = garfoEsquerdo(i);
			int dir = garfoDireito(i);
			boxLine(String.format("Filósofo %-2d usa: Garfo %d (esquerdo) e Garfo %d (direito)", (i + 1), esq, dir));
		}
		boxLineBlank();
		boxBottom();
	}

	// ===== Helpers de formatação/caixa =====

	// Largura interna (entre as bordas) = 62, com 1 espaço de margem em cada lado.
	// Formato de linha: "║ " + <conteúdo com largura 60> + " ║"
	// Linha superior/inferior: 62 '═' entre os cantos.
	private static final int INNER_CONTENT = 60;
	private static final int INNER_WITH_MARGINS = INNER_CONTENT + 2; // espaços laterais dentro da caixa

	private void boxTop() {
		System.out.println("╔" + repeat('═', INNER_WITH_MARGINS) + "╗");
	}

	private void boxSep() {
		System.out.println("╠" + repeat('═', INNER_WITH_MARGINS) + "╣");
	}

	private void boxBottom() {
		System.out.println("╚" + repeat('═', INNER_WITH_MARGINS) + "╝");
	}

	private void boxLine(String content) {
		// aceita qualquer string; faz pad para caber em 60
		System.out.println("║ " + padRight(fit(content, INNER_CONTENT), INNER_CONTENT) + " ║");
	}

	private void boxLineCenter(String content) {
		String s = center(fit(content, INNER_CONTENT), INNER_CONTENT);
		System.out.println("║ " + s + " ║");
	}

	private void boxLineBlank() {
		System.out.println("║ " + repeat(' ', INNER_CONTENT) + " ║");
	}

	// Variante que trunca com precisão para não estourar a largura
	private void boxLineFit(String content) {
		System.out.println("║ " + padRight(fit(content, INNER_CONTENT), INNER_CONTENT) + " ║");
	}

	// === utilidades de texto ===
	private static String repeat(char ch, int n) {
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++)
			sb.append(ch);
		return sb.toString();
	}

	private static String padRight(String s, int width) {
		if (s.length() >= width)
			return s;
		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < width)
			sb.append(' ');
		return sb.toString();
	}

	private static String center(String s, int width) {
		if (s.length() >= width)
			return s;
		int totalPad = width - s.length();
		int left = totalPad / 2;
		int right = totalPad - left;
		return repeat(' ', left) + s + repeat(' ', right);
	}

	// Garante não ultrapassar a largura; se necessário, trunca.
	private static String fit(String s, int width) {
		if (s.length() <= width)
			return s;
		// tenta preservar o fim sem quebrar muito: usa "…" no final
		if (width <= 1)
			return s.substring(0, width);
		return s.substring(0, width - 1) + "…";
	}

	// Estado textual no estilo antigo com largura fixada p/ montar o vetor
	private String estadoFix(int i) {
		return switch (filosofos[i]) {
		case PENSANDO -> "PENSANDO";
		case COMENDO -> "COMENDO ";
		case FOME -> "FOME     ";
		default -> "???      ";
		};
	}
}
