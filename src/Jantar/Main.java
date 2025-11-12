package Jantar;

public class Main {
    public static void main(String[] args) {
        Mesa mesa = new Mesa();
        Thread[] threads = new Thread[5];

        System.out.println("========================================");
        System.out.println(" JANTAR DOS FILÓSOFOS - INICIANDO");
        System.out.println("========================================\n");

        System.out.println("ESTADO INICIAL:");
        mesa.imprimeEstadosFilosofos();
        mesa.imprimeGarfos();
        System.out.println();

        for (int filosofo = 0; filosofo < 5; ++filosofo) {
            threads[filosofo] = new Filosofos("Filosofo_" + filosofo, mesa, filosofo);
            threads[filosofo].start();
        }

        int tempoExecucao = 10000; // 10 segundos
        try {
            System.out.println("Simulação rodando por " + tempoExecucao / 1000 + " segundos...\n");
            Thread.sleep(tempoExecucao);
            System.out.println("\n========================================");
            System.out.println(" TEMPO ESGOTADO - ENCERRANDO");
            System.out.println("========================================\n");

            for (Thread t : threads) {
                t.interrupt();
            }
            for (Thread t : threads) {
                t.join(1000L);
            }

            mesa.imprimirEstatisticasFinais();
        } catch (InterruptedException e) {
            System.err.println("Simulação interrompida!");
        }

        System.out.println("\n========================================");
        System.out.println(" PROGRAMA FINALIZADO");
        System.out.println("========================================");
        System.exit(0);
    }
}