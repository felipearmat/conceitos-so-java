////////////////////////////////////////////////
/* SSC0541 - Sistemas Operacionais            */
////////////////////////////////////////////////
/* Confecção de um Jogo - Jogo de Ação 2D     */
/* Alunos:                                    */
/* Felipe Araújo Matos               5968691  */
/* Glauco Henrique Borges da Costa  10295134  */
/* Hugo Azevedo Vitulli             10295221  */
/* Reinaldo Mizutani                 7062145  */
////////////////////////////////////////////////

/* Esse trabalho pretende fixar conceitos relacionados à Sistemas Operacionais, 
mais especificamente semáforos, threads e memória (segmentação, fragmentação 
ou coerência de cache). Para isso os membros desse grupo deverão desenvolver 
um jogo que envolva os conceitos em questão, em qualquer linguagem desejada.
Para este trabalho optou-se pelo desenvolvimento de um jogo 2D do tipo shooter 
de ação, onde inimigos aleatórios serão gerados e uma nova leva de inimigos é 
gerada cada vez que a leva anterior for totalmente exterminada. O jogo acaba 
assim que o jogador ficar sem pontos de vida. */

/* 
ESPECIFICAÇÔES DO PROJETO:
- Nesse jogo teremos quatro threads sendo aplicadas e utilizadas, uma deverá ser 
responsável por desenhar e redesenhar os personagens na tela, outra será responsável 
por mover os monstros em várias direções, uma terceira thread será utilizada para 
gerar as combinações de inimigos e gerenciar o arquivo de dados e a quarta será responsável
por gerenciar outros recursos do jogo, como os tiros efetuados, o loop e fim do jogo, 
pontuação e outras possíveis facilidades a serem implementadas (a última thread não pode 
ser implementada por falta de tempo). 
- Utilizaremos um monitor para evitar que personagens do jogo se sobreponham, se em algum 
momento dois monstros se sobreporem eles tentarão sair desda condição. O monitor de posição
também será responsável por impedir que monstros saiam da área de jogo.
- Utilizaremos um arquivo de tamanho fixo onde serão geradas as combinações de inimigos 
previamente. Para que o conceito de gerenciamento de memória seja utilizado, as combinações 
de inimigos deverão possuir diferentes tamanhos e estas serão gravadas no arquivo de combinações 
até que não seja possível incluir mais combinações em um primeiro momento. As combinações serão 
escolhidas aleatoriamente e, após a utilização de uma combinação, esta será removida do arquivo 
de combinações. Após a utilização de três combinações, novas combinações serão geradas e serão 
adicionadas no arquivo utilizando o algoritmo first-fit, até não ser possível mais adicionar novas 
combinações.
*/

//Bibliotecas utilizadas na aplicação
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;

//Classe principal da aplicação
public class main {
    //Variáveis que definem o tamanho do board.
    public static final int BOARDX = 400;
    public static final int BOARDY = 400;

    public static void main(String[] args) throws IOException {
        //Cria um JFrame (janela) onde o jogo será executado
        JFrame mainFrame = new JFrame("GUI");
        //Define comportamento padrão do JFrame ao clicar em fechar janela
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Determina a posição (x e y) da janela e seu tamanho (w, h)
        mainFrame.setBounds(0, 0, BOARDX, BOARDY);
        mainFrame.setLayout(new BorderLayout());
        //Cria um mapa de pontos mutáveis com chaves de strings para posicionamento
        //dos objetos no frame do jogo.
        Map<String, MutablePoint> locations = new HashMap <>();
        //Cria um monitor para os monstros que observa o mapa de posições
        MonsterTracker mt = new MonsterTracker(locations);
        //Cria um container (campo) onde será adicionado "tabuleiro" do jogo
        GameBoard paintPanel = new GameBoard(mt);
        //Insere o tabuleiro dentro da janela do jogo e o centraliza
        mainFrame.add(paintPanel, BorderLayout.CENTER);
        //Torna a janela principal visível
        mainFrame.setVisible(true);
        //Cria um objeto fileReader que controlará o arquivo que simula a
        //memória com alocação dinâmica.
        FileReader fileControl = new FileReader("teste.txt");
        //Cria o arquivo do fileControl, sobrescreve caso já exista
        fileControl.createFile();
        //Preenche o arquivo do filecontrol com as sequências iniciais
        fileControl.fillFile();

        //ria um novo thread chamado monsterUpdater, que controla o arquivo 
        //de monstros e a geração de monstros no mapa
        Thread monsterUpdater = new Thread(new Runnable(){
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        //Pausa entre execuções do thread, quanto menor mais rapido
                        Thread.sleep(100);
                    } catch (InterruptedException e){
                        //Em caso de erro no thread
                        e.printStackTrace();
                        System.out.println("Erro no thread de monstros.");
                    }
                    paintPanel.checkBoard(fileControl);
                }
            }
        });
        //Cria um novo thread chamado posUpdater, que é responsável
        //por mover as peças do jogo
        Thread posUpdater = new Thread(new Runnable(){
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                        System.out.println("Erro no thread de posições.");
                    }
                    paintPanel.movePoints();
                }
            }
        });
        //Cria um novo thread chamado boardUpdater, que é responsável
        //por atualizar o tabuleiro do jogo.
        Thread bgUpdater = new Thread(new Runnable(){
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                        System.out.println("Erro no thread do background.");
                    }
                    paintPanel.repaint();
                }
            }
        });
        //Cria um novo thread chamado gameUpdater, que é responsável
        //por atualizar as funcionalidades do jogo (não implementado)
        Thread gameUpdater = new Thread(new Runnable(){
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                        System.out.println("Erro no thread do jogo.");
                    }
                    //TODO: Implementar rotina que interrompe as outras threads e
                    //mata todo o processo do jogo verificando a vida do boneco.
                }
            }
        });
        //Inicia threads
        posUpdater.start();
        bgUpdater.start();
        monsterUpdater.start();
        // gameUpdater.start();
    }
}

//Classe que criar o "tabuleiro" de jogo
class GameBoard extends JPanel implements KeyListener {
    //Cria variaveis estaticas publicas para referência em outros métodos
    public static final int BHEIGHT = 300;
    public static final int BWIDTH = 400;
    public static final int PWIDTH = 30;
    public static final int PHEIGHT = 10;
    public static final int PSPEED = 2;
    //Contador de "limpeza" do board
    public int spawCounter = 0;
    //Posições iniciais do player
    private int dx = BWIDTH / 2;
    private int dy = BHEIGHT;
    //Cria boneco do jogador
    private Rectangle2D player = new Rectangle2D.Double(dx, dy, PWIDTH, PHEIGHT);
    //Cria o raio laser, que fica fora do mapa, na regiao de objetos mortos
    private Rectangle2D laserBeam = new Rectangle2D.Double(9999, 9999, 0, 0);

    //Cria o monitor para os monstros
    private MonsterTracker monitor;

    //Construtor do tabuleiro, adiciona as interfaces de escuta de teclado
    public GameBoard(MonsterTracker mt) {
        this.addKeyListener(this);
        this.setBackground(Color.white);
        this.setFocusable(true);
        this.monitor = mt;
        this.spawCounter = 0;
    }

    @Override
    //Sobrescreve Método para desenho de tela, que inclui verificação de inimigos 
    //mortos e redesenho de cada tank baseado em cada ponto mutavel presente no
    //monitor do tabuleiro
    protected void paintComponent(Graphics grphcs) {
        cleanDead();
        super.paintComponent(grphcs);
        Graphics2D gr = (Graphics2D) grphcs;
        gr.draw(player);
        gr.draw(laserBeam);
        for(MutablePoint p: monitor.getLocations().values()) {
            if (p.alive)
                drawMonster(grphcs, p.x, p.y, p.width, p.height);
        }
    }

    //Método que retorna a quantidade de monstros vivos no tabuleiro
    public int getLiveMonsters() {
        int count = 0;
        for(String id: monitor.getLocations().keySet()) {
            MutablePoint p = monitor.getLocation(id);
            if(p.alive) {
                count++;
            }
        }
        return count;
    }

    //Método para criação de monstros
    public void createMonster(int size) {
        MutablePoint p = new MutablePoint(size*20, size*20, size);
        monitor.insert("monster" + (getLiveMonsters() + 1), p);   
        System.out.println("Tamanho do board:" + getLiveMonsters());
    }

    //Método que move os pontos mutáveis no tabuleiro.
    public void movePoints(){
        for(String id: monitor.getLocations().keySet()) {
            MutablePoint p = monitor.getLocation(id);
            if(p.alive) {
                monitor.setLocation(id, p.x + p.speed * p.velX, p.y + p.speed * p.velY, true);
            }
        }
    }

    //Método para verificação de objetos mortos, verifica se algum objeto está
    //sobreposto pelo raiolaser e, caso esteja, muda seu status de vivo para
    //false e o posiciona fora do tabuleiro
    private void cleanDead() {
        //Para cada item dentro do monitor, associa um ponto mutavel à sua posição
        for(String id: monitor.getLocations().keySet()) {
            MutablePoint p = monitor.getLocation(id);
            //Se a posição do ponto mutável for sobreposta por laserbeam muda
            //seu status para morto e o posiciona fora do tabuleiro.
            if(overlaps(p.x, p.y, p.width, p.height, laserBeam)) {
                monitor.setLocation(id, 9999, 9999, false);
            }
            //Se a posição do tanque do jogador se sobrepor a posição do objeto
            //no monitor, remove-se o observador de teclado do tabuleiro.
            //TODO: Implementar perda de vida no caso de colisão e gameOver
            if(overlaps(p.x, p.y, p.width, p.height, player)) {
                //this.removeKeyListener(this);   
            }
        }
    }

    //Método que verifica quando um objeto está sobrepondo outro objeto
    private boolean overlaps(int x, int y, int width, int height, Rectangle2D r) {
        return x < r.getX() + r.getWidth() && x + width > r.getX()
        && y < r.getY() + r.getHeight() && y + height > r.getY();
    }

    //Método que desenha o monstro na tela
    private void drawMonster(Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.yellow);
        g.draw3DRect(x, y, width, height, true);
    }

    //Método para checagem de monstros vivos e criação de novos monstros.
    public void checkBoard(FileReader fr) {
        if(getLiveMonsters() == 0) {
            int[] res = fr.consumeComb();    
            for(int i: res) {
                createMonster(i);
            }
            spawCounter++;
            System.out.println("Contador de spaw: " + spawCounter);
            if(spawCounter>2) {
                spawCounter = 0;
                System.out.println("Arquivo preenchido." + spawCounter);
                try {
                    fr.fillFile();
                } catch (IOException ex) {
                    System.out.println("Erro no preenchimento do arquivo.");
                    Logger.getLogger(FileReader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    //Métodos para controle de eventos de teclado.
    @Override
    public void keyTyped(KeyEvent e) {
        shoot();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        moveRec(e);
        repaint();
    }

    //Método para esconder laser apos pressionar uma tecla
    @Override
    public void keyReleased(KeyEvent e) {
        laserBeam.setRect(9999, 9999, 0, 0);  //hide it
        repaint();
    }

    //Método para lançamento de tiros
    public void shoot() {
        laserBeam.setRect(dx + PWIDTH/2, 0, 2, dy);
    }

    //Método que determina o evento de acordo com a tecla pressionada
    public void moveRec(KeyEvent evt) {
        int temp;
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            temp = dx - PSPEED;
            if(temp < 0)
                temp = 0;
            dx = temp;
            break;
            case KeyEvent.VK_RIGHT:
            temp = dx + PSPEED;
            if(temp > (main.BOARDX - 50))
                temp = main.BOARDX - 50;
            dx = temp;
            break;
            case KeyEvent.VK_UP:
            temp = dy - PSPEED;
            if (temp < 0)
                temp = 0;
            dy = temp;
            break;
            case KeyEvent.VK_DOWN:
            temp = dy + PSPEED;
            if (temp > (main.BOARDY - 50))
                temp = (main.BOARDY - 50);
            dy = temp;
            break;
        }
        player.setRect(dx, dy, PWIDTH, PHEIGHT);
    }
}

//Classe para os pontos mutáveis que serão controlados no tabuleiro
class MutablePoint {
    //TODO: Tornar atributos privados e criar geter e seters para atributos
    public int x, y, speed, velX, velY, width, height;
    public boolean alive;
    
    //Construtor padrão para a criação de um ponto mutável sem argumentos.
    public MutablePoint() {
        this(0, 0, 1);
    }

    //Sobrecarga do construtor baseado em ints de posição e de tamanho.
    public MutablePoint(int x, int y, int size) {
        this.width = size*10;
        this.height = size*10;
        this.x = x;
        this.y = y;
        this.alive = true;
        this.speed = size * 2;
        this.velX = 1;
        this.velY = ThreadLocalRandom.current().nextInt(-1, 2);
    }

    //Sobrecarga de construtor para chamada passando um MutablePoint.
    public MutablePoint(MutablePoint p) {
        this.x = p.x;
        this.y = p.y;
        this.alive = p.alive;
        this.speed = p.speed;
        this.velX = p.velX;
        this.velY = p.velY;
        this.width = p.width;
        this.height = p.height;
    }

    //Geters e seters para mutablePoints
    public int getX(){
        return this.x;  
    }
    public int getY(){
        return this.y;   
    }
    public int getWidth(){
        return this.width;  
    }
    public int getHeight(){
        return this.height;  
    }

    //Métodos para mudança de direção
    public void turnAroundX() {
        this.velX *= -1;
    }
    public void turnAroundY() {
        this.velY *= -1;
    }
}

//Classe para o monitor de veículos
class MonsterTracker {
    //O monitor possui um hash map para controle dos objetos monitorados
    private Map<String, MutablePoint> locations;
    //O construtor do monitor recebe um hash map e faz um deepcopy (copia todos 
    //os atributos e outros objetos referenciados pelo objeto original) ao
    //invés de copiar apenas sua  referência (shallow copy)
    public MonsterTracker(Map<String, MutablePoint> locations) {
        this.locations = deepCopy(locations);
    }
    //Método que verifica quantos elementos existem no monitor
    public int getSize() {
        return locations.size();
    }
    //Método getLocations é sincronizado, ou seja, só pode ser acessado por uma 
    //thread por vez. Retorna uma deepcopy do hashmap de posições.
    public synchronized Map<String, MutablePoint> getLocations() {
        return deepCopy(locations);
    }
    //Método getLocation também é sincronizado. Retorna um novo objeto com as 
    //mesmas características do objeto com o ID da chamada. Se o objeto tiver
    //posição null, ou seja não existir, retorna null.
    public synchronized MutablePoint getLocation(String id) {
        MutablePoint obj = locations.get(id);
        return obj == null ? null : new MutablePoint(obj);
    }
    //Método setLocation é sincronizado. Recebe um id, uma posição x, uma posição y
    //e um booleano "vivo", muda objeto para novas posições e determina seu estado.
    //Se o objeto não existir lança uma excessão.
    public synchronized void setLocation(String id, int x, int y, boolean alive) {
        MutablePoint obj = locations.get(id);
        if (obj == null)
            throw new IllegalArgumentException("No such ID: " + id);
        //Se o monstro tentar ser movido para o canto do mapa
        //ele muda de direção.
        if (x + obj.getWidth() >= main.BOARDX) {
            x = main.BOARDX-obj.getWidth();
            obj.turnAroundX(); 
        }
        if (x < 0) {
            x += x * -2;
            obj.turnAroundX(); 
        }
        if (y + obj.getHeight() >= main.BOARDY) {
            y = main.BOARDY-obj.getHeight();
            obj.turnAroundY(); 
        }
        if (y < 0) {
            y += y * -2;
            obj.turnAroundY(); 
        }

        //Toda vez que um ponto é movido ele gera um número aleatório
        //entre 0 e 3
        int ran = ThreadLocalRandom.current().nextInt(0, 4);

        //Se o monstro gerar o número aleatório 0, ele muda de
        //direção, mas sem movimentos bruscos.
        if(ran == 0){
            int dir = ThreadLocalRandom.current().nextInt(-1, 2);
            if(obj.velX == 0){
                obj.velX += dir * obj.velY;
            } 
            else if(obj.velY == 0){
                obj.velY -= dir * obj.velX;
            }
            else if(obj.velX == obj.velY){
                if(dir > 0 )
                    obj.velY = 0;
                else if(dir < 0 )
                    obj.velX = 0;
            }
            else{
                if(dir > 0 )
                    obj.velX = 0;
                else if(dir < 0 )
                    obj.velY = 0;
            }  
        }

        //Tratador de colisão entre monstros. Lógica simples, ainda sujeita a
        //sttutering entre monstros.
        for(String id2: locations.keySet()) {
            MutablePoint obj2 = locations.get(id2);
            //Se o objeto não for o próprio objeto (autocolisão) então muda de direção
            if(!id.equals(id2) && overlaps(obj.x, obj.y, obj.width, obj.height, obj2)) {
                //Nova direção é aleatória, cria variaveis que receberão as novas direções
                int temp1;
                int temp2;
                //Objeto não deve ficar parado (velX = 0 e velY = 0)
                do{
                    temp1 = ThreadLocalRandom.current().nextInt(-1, 2);
                    temp2 = ThreadLocalRandom.current().nextInt(-1, 2);
                }while(temp1 == 0 && temp2 == 0);
                //Faz obj1 e obj2 mudarem ambos para direções opostas
                obj.velX = temp1;
                obj2.velX = temp1*-1;
                obj.velY = temp2;
                obj2.velY = temp2*-1;
                //Calcula nova posição para qual o obj1 vai se mover
                x = obj.x + obj.velX * obj.speed;
                y = obj.y + obj.velY * obj.speed;
            }
        }
        //Move o objeto para a direção esperada.
        obj.x = x;
        obj.y = y;
        obj.alive = alive;
    }

    //Método para calcular sobreposição entre objetos pelo monitor.
    private boolean overlaps(int x, int y, int width, int height, MutablePoint p) {
        return x < p.getX() + p.getWidth() && x + width > p.getX()
        && y < p.getY() + p.getHeight() && y + height > p.getY();
    }

    //Método deepCopy, cria um novo objeto do tipo hashmap e copia todos os objetos
    //contidos no mapa original para o novo mapa. Retorna o mapa copiado.
    private static Map<String, MutablePoint> deepCopy(Map<String, MutablePoint> m) {
        Map<String, MutablePoint> result = new HashMap<>();
        for (String id : m.keySet())
            result.put(id, new MutablePoint(m.get(id)));
        return result;
    }

    //Método para inserção de monstros no monitor.
    public void insert(String string, MutablePoint p) {
        locations.put(string, p);
    }
}

//Classe para leitura do arquivo físico que simula um espaço de memória
class FileReader {
    //String com letras e números para geração aleatória de palavras
    private final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    //Gera um aleatório seguro para gerar a nova palavra 
    //TODO:Isso é overkill, talvez mudar se der tempo
    private SecureRandom rnd = new SecureRandom();
    //String que possui caminho para o arquivo a ser controlado
    private final String filePath;
    //Construtor da classe, recebe uma string com caminho do arquivo
    public FileReader(String path) {
        this.filePath = path;
    }
    //Método base para leitura de arquivo e retornar seu conteúdo como string
    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
    //Método para consumo de combinações do arquivo
    public int[] consumeComb() {    
        String[] content;
        int[] myIntArray = {1,2,3,3};
        try {
            content = readFile(filePath, Charset.defaultCharset()).split("_");
        } catch (IOException ex) {
            System.out.println("Erro no consumo de combinações.");
            Logger.getLogger(FileReader.class.getName()).log(Level.SEVERE, null, ex);
            return myIntArray;   
        }
        //Gera uma arrayList com o conteúdo do arquivo
        ArrayList<String> listContent = new ArrayList<>(Arrays.asList(content));
        int iterator = 0;
        //Tenta aleatoriamente achar uma sequência não vazia no arquivo, se tentar
        //uma quantidade de vezes maior que o dobro da quantidade de sequências
        //desiste de consumir uma sequência.
        while (iterator < 2*listContent.size()) {
            //Gera um número aleatório para acessar as sequências disponíveis
            int ran =  ThreadLocalRandom.current().nextInt(0, listContent.size());
            String s = listContent.get(ran);
            //Se for uma sequência que não está vazia
            if (!s.startsWith(" ")) {
                //Pega os últimos quatro dígitos da sequência
                myIntArray[0] = Character.getNumericValue(s.charAt(s.length()-1));
                myIntArray[1] = Character.getNumericValue(s.charAt(s.length()-2));
                myIntArray[2] = Character.getNumericValue(s.charAt(s.length()-3));
                myIntArray[3] = Character.getNumericValue(s.charAt(s.length()-4));
                //Transforma a sequência atual em uma sequência vazia
                listContent.set(ran, createSpace(s.length()));
                //Concatena a sequência em uma string
                String temp = String.join("_", listContent);
                try {
                    //Escreve a sequência no arquivo, juntando espaços vazios
                    //consecutivos
                    Files.write(Paths.get(filePath), temp.replace(" _ ", "   ").getBytes());
                //Se ocorrer erro ao gravar o arquivo imprime uma mensagem e loga o erro
                } catch (IOException ex) {
                    System.out.println("Erro ao gravar arquivo.");
                    Logger.getLogger(FileReader.class.getName()).log(Level.SEVERE, null, ex);
                }
                //Retorna a array descrita pelo arquivo
                return myIntArray;
            }
            iterator++;
        }
        //Retorna uma array padrão e avisa erro.
        System.out.println("Não foi possível pegar uma sequência do arquivo.");
        return myIntArray;
    }

    //Método para criação do arquivo, cria uma sequência vazia de 200 caracteres
    public void createFile() throws IOException {
        String data = createSpace(200);
        data += "_";
        Files.write(Paths.get(filePath), data.getBytes());
    }

    //Método para preenchimento do arquivo
    public void fillFile() throws IOException {
        //Pega o conteúdo do arquivo e o separa usando _ como referência
        String[] content = readFile(filePath, Charset.defaultCharset()).split("_");
        //Tansforma conteúdo em uma arraylist para fácil inserção
        ArrayList<String> listContent = new ArrayList<String>(Arrays.asList(content));
        String temp;
        int iterator = 0;
        //Atravessa a lista vendo todos as palavras contidas
        while (iterator < listContent.size()) {
            //Cria sequência aleatória para inserir no arquivo
            temp = createComb();
            //Pega string para saber tamanho e se é vazia
            String s = listContent.get(iterator);
            //Verifica se a string é vazia e se seu comprimento é
            //maior que a da string gerada
            if (s.startsWith(" ") && s.length() > temp.length()) {
                //Adiciona a palavra na sequência gerada
                listContent.add(iterator, temp);
                //Substitui a string vazia por uma menor
                temp = createSpace(s.length() - temp.length());
                listContent.set(iterator+1, temp);
            //Se a string encontrada é do tamanho da string vazia
            }else if (s.startsWith(" ") && s.length() == temp.length()){
                //Substitui a string encontrada pela gerada
                listContent.set(iterator, temp); 
            }
            //Avança iterador
            iterator++;
        }
        //Concatena as strings da lista atualizada
        temp = String.join("_", listContent);
        //Escreve string no arquivo concatenando epaços vazios
        Files.write(Paths.get(filePath), temp.replace(" _ ", "   ").getBytes());
    }

    //Método que gera string de espaços vazios.
    public String createSpace(int len) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < len; i++) 
            sb.append(" ");
        return sb.toString();
    }

    //Método que gera string aleatória com tamanhos aleatórios
    //para os monstros
    public String createComb() {
        String temp = randomString();
        int num1 =  ThreadLocalRandom.current().nextInt(1, 4);
        int num2 =  ThreadLocalRandom.current().nextInt(1, 4);
        int num3 =  ThreadLocalRandom.current().nextInt(1, 4);
        int num4 =  ThreadLocalRandom.current().nextInt(1, 4);
        String result = temp + String.valueOf(num1) + String.valueOf(num2) + String.valueOf(num3) + String.valueOf(num4); 
        return result;
    }

    //Método para criar uma string aleatória baseada na string de
    //caracteres e números da classe
    public String randomString(){
        int len = ThreadLocalRandom.current().nextInt(3, 22);
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++) 
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }
}