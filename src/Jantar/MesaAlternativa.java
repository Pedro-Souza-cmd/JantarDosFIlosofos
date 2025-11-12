package Jantar;

import java.util.concurrent.Semaphore;

public class MesaAlternativa {
	// Estados
	static final int PENSANDO = 1;
	static final int COMENDO = 2;
	static final int FOME = 3;

	// Config
	static final int NR_FILOSOFOS = 5;
	static final int MAX_TRACE = 50; // quantas execuções detalhar
	static final int TRACE_DELAY_MS = 15; // pequeno intervalo entre mensagens

	// Recursos/estados
	boolean[] garfos = new boolean[NR_FILOSOFOS];
	int[] filosofos = new int[NR_FILOSOFOS];

	// Métricas
	int[] tentativas = new int[NR_FILOSOFOS]; // esperas nesta "rodada" (auxiliar)
	int[] tentativasTotal = new int[NR_FILOSOFOS]; // esperas acumuladas
	int[] contador = new int[NR_FILOSOFOS]; // refeições por filósofo

	private int totalRefeicoes = 0; // todas as refeições
	private int contadorExecucoesDetalhadas = 0; // quantas execuções foram impressas (<= MAX_TRACE)
	private boolean avisouFimTrace = false;

	// Controle global: apenas 1 filósofo por vez (regra que você pediu)
	private final Semaphore mordomo = new Semaphore(1);

	public MesaAlternativa() {
		for (int i = 0; i < NR_FILOSOFOS; ++i) {
			garfos[i] = true;
			filosofos[i] = PENSANDO;
			tentativas[i] = 0;
			tentativasTotal[i] = 0;
			contador[i] = 0;
		}
	}

	// ===== Interface usada pelo thread do filósofo =====
	// Conta espera se o semáforo não estiver livre
	public void entrarNoRefeitorio(int filosofo) throws InterruptedException {
		if (!mordomo.tryAcquire()) {
			tentativas[filosofo]++;
			tentativasTotal[filosofo]++;
			mordomo.acquire();
		}
	}

	public void sairDoRefeitorio() {
		mordomo.release();
	}

	// ===== Núcleo: pegar/devolver garfos, com logs passo-a-passo =====
	public synchronized void pegarGarfos(int filosofo) {
		filosofos[filosofo] = FOME;
		int esq = garfoEsquerdo(filosofo);
		int dir = garfoDireito(filosofo);

		// Espera pelos dois garfos (e conta esperas por garfos)
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

		// ===== Execução detalhada: cabeçalho + anúncio de início =====
		if (contadorExecucoesDetalhadas < MAX_TRACE) {
			contadorExecucoesDetalhadas++;
			System.out.println("\n========== Execução #" + contadorExecucoesDetalhadas + " ==========");
			System.out.printf("Filósofo %-2d parou de pensar e vai começar a comer%n", (filosofo + 1));
			traceDelay();
		}

		// Pega o garfo esquerdo (marca e loga)
		garfos[esq] = false;
		if (contadorExecucoesDetalhadas <= MAX_TRACE) {
			System.out.printf("Filósofo %-2d pegou garfo %-2d (esquerdo)%n", (filosofo + 1), esq);
			traceDelay();
		}

		// Pega o garfo direito (marca e loga)
		garfos[dir] = false;
		if (contadorExecucoesDetalhadas <= MAX_TRACE) {
			System.out.printf("Filósofo %-2d pegou garfo %-2d (direito)%n", (filosofo + 1), dir);
			traceDelay();
		}

		// Entra no estado COMENDO e registra refeição
		filosofos[filosofo] = COMENDO;
		contador[filosofo]++;
		totalRefeicoes++;

		// Anuncia início efetivo de comer + estados/garfos alinhados
		if (contadorExecucoesDetalhadas <= MAX_TRACE) {
			System.out.printf("Filósofo %-2d começou a comer%n", (filosofo + 1));
			imprimeEstadosFilosofos();
			imprimeGarfos();
			System.out.println();
		} else if (!avisouFimTrace && contadorExecucoesDetalhadas == MAX_TRACE) {
			avisouFimTrace = true;
			System.out.println("\n========== Limite de " + MAX_TRACE
					+ " execuções detalhadas atingido. Trace pausado. ==========\n");
		}

		// zera tentativas da rodada (opcional)
		tentativas[filosofo] = 0;
	}

	public synchronized void returningGarfos(int filosofo) {
		int esq = garfoEsquerdo(filosofo);
		int dir = garfoDireito(filosofo);

		// Mensagens de término (apenas enquanto ainda estamos detalhando)
		if (contadorExecucoesDetalhadas <= MAX_TRACE) {
			System.out.printf("Filósofo %-2d terminou de comer e voltou a pensar%n", (filosofo + 1));
			traceDelay();
		}

		// Atualiza estado e libera ambos os garfos
		filosofos[filosofo] = PENSANDO;
		garfos[esq] = true;
		garfos[dir] = true;

		if (contadorExecucoesDetalhadas <= MAX_TRACE) {
			System.out.printf("Filósofo %-2d devolveu os garfos %d e %d à mesa%n", (filosofo + 1), esq, dir);
		}

		notifyAll();
	}

	// ===== Mapeamento clássico dos garfos =====
	public int garfoEsquerdo(int filosofo) {
		return filosofo;
	}

	public int garfoDireito(int filosofo) {
		return (filosofo + 1) % NR_FILOSOFOS;
	}

	// ===== Impressões alinhadas (estado corrente) =====
	public void imprimeEstadosFilosofos() {
		System.out.print("Filósofos  = [ ");
		for (int i = 0; i < NR_FILOSOFOS; ++i) {
			String estado = switch (filosofos[i]) {
			case PENSANDO -> "PENSANDO";
			case COMENDO -> "COMENDO ";
			case FOME -> "FOME     ";
			default -> "???      ";
			};
			System.out.printf("%d:%-9s ", (i + 1), estado);
		}
		System.out.println("]");
	}

	public void imprimeGarfos() {
		System.out.print("Garfos     = [ ");
		for (int i = 0; i < NR_FILOSOFOS; ++i) {
			String est = garfos[i] ? "LIVRE" : "OCUPADO";
			System.out.printf("%d:%-8s ", i, est);
		}
		System.out.println("]");
	}

	// ===== Painel final com bordas alinhadas =====
	public void imprimirEstatisticasFinais() {
		// totais
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

		boolean houveStarvation = false;
		for (int i = 0; i < NR_FILOSOFOS; i++) {
			if (tentativasTotal[i] > contador[i] * 5) {
				houveStarvation = true;
				break;
			}
		}

		// estado final (estilo antigo)
		StringBuilder estados = new StringBuilder("Filósofos = [ ");
		for (int i = 0; i < NR_FILOSOFOS; i++)
			estados.append(estadoFix(i)).append(" ");
		estados.append("]");

		StringBuilder garfosSb = new StringBuilder("Garfos    = [ ");
		for (int i = 0; i < NR_FILOSOFOS; i++)
			garfosSb.append(garfos[i] ? "LIVRE   " : "OCUPADO ");
		garfosSb.append("]");

		// Caixa alinhada
		boxTop();
		boxLineCenter("ESTATÍSTICAS FINAIS DO JANTAR DOS FILÓSOFOS");
		boxSep();

		boxLineBlank();
		boxLine("DESEMPENHO INDIVIDUAL:");
		boxLine("─────────────────────");
		for (int i = 0; i < NR_FILOSOFOS; i++) {
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

	// ===== Helpers de trace =====
	private void traceDelay() {
		try {
			Thread.sleep(TRACE_DELAY_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// ===== Helpers de caixa/format =====
	private static final int INNER_CONTENT = 60; // largura útil dentro da caixa
	private static final int INNER_WITH_MARGINS = INNER_CONTENT + 2;

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
		System.out.println("║ " + padRight(fit(content, INNER_CONTENT), INNER_CONTENT) + " ║");
	}

	private void boxLineCenter(String content) {
		String s = center(fit(content, INNER_CONTENT), INNER_CONTENT);
		System.out.println("║ " + s + " ║");
	}

	private void boxLineBlank() {
		System.out.println("║ " + repeat(' ', INNER_CONTENT) + " ║");
	}

	private void boxLineFit(String content) {
		System.out.println("║ " + padRight(fit(content, INNER_CONTENT), INNER_CONTENT) + " ║");
	}

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

	private static String fit(String s, int width) {
		if (s.length() <= width)
			return s;
		if (width <= 1)
			return s.substring(0, width);
		return s.substring(0, width - 1) + "…";
	}

	// Texto fixo dos estados para a linha de "ESTADO FINAL"
	private String estadoFix(int i) {
		return switch (filosofos[i]) {
		case PENSANDO -> "PENSANDO";
		case COMENDO -> "COMENDO ";
		case FOME -> "FOME     ";
		default -> "???      ";
		};
	}
}
