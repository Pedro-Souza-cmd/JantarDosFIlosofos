package Jantar;

public class Filosofos extends Thread {
    static final int TEMPO_MAXIMO = 100;
    Mesa mesa;
    int filosofo;

    public Filosofos(String nome, Mesa mesadejantar, int fil) {
        super(nome);
        this.mesa = mesadejantar;
        this.filosofo = fil;
    }

    public void run() {
        try {
            while (!Thread.interrupted()) {
                int tempo = (int) (Math.random() * TEMPO_MAXIMO);
                pensar(tempo);

                boolean pegou = false;
                try {
                    mesa.entrarNoRefeitorio(filosofo); // << agora passa o id
                    mesa.pegarGarfos(filosofo);
                    pegou = true;

                    tempo = (int) (Math.random() * TEMPO_MAXIMO);
                    comer(tempo);
                } finally {
                    if (pegou) {
                        mesa.returningGarfos(filosofo);
                    }
                    mesa.sairDoRefeitorio();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Filósofo " + (filosofo + 1) + " interrompido e finalizando.");
        }
    }

    public void pensar(int tempo) throws InterruptedException { sleep(tempo); }
    public void comer(int tempo)  throws InterruptedException { sleep(tempo); }
}
