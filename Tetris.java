import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * Тетрис на Java (Swing). Запуск: java Tetris.java
 *
 * Управление:
 *   стрелки влево/вправо — двигать фигуру
 *   стрелка вверх        — повернуть
 *   стрелка вниз         — опустить на клетку быстрее
 *   пробел               — уронить сразу до самого низа
 */
public class Tetris extends JPanel implements ActionListener, KeyListener {

    // ---------- НАСТРОЙКИ ----------
    static final int CELL = 30;            // размер одной клетки в пикселях
    static final int COLS = 10;            // сколько клеток по ширине
    static final int ROWS = 20;            // сколько клеток по высоте
    static final int WIDTH = CELL * COLS;  // ширина окна
    static final int HEIGHT = CELL * ROWS; // высота окна
    static final int FALL_DELAY = 500;     // через сколько миллисекунд фигура падает на клетку ниже

    // Семь классических фигур. 1 — клетка есть, 0 — пусто.
    static final int[][][] SHAPES = {
        {{1, 1, 1, 1}},      // I — палка

        {{1, 1},
         {1, 1}},            // O — квадрат

        {{0, 1, 0},
         {1, 1, 1}},         // T

        {{1, 0, 0},
         {1, 1, 1}},         // J

        {{0, 0, 1},
         {1, 1, 1}},         // L

        {{0, 1, 1},
         {1, 1, 0}},         // S

        {{1, 1, 0},
         {0, 1, 1}},         // Z
    };

    // Цвета (красный, зелёный, синий). Номер цвета = номер фигуры + 1,
    // потому что 0 в поле означает "пусто".
    static final Color[] COLORS = {
        Color.BLACK,                 // 0 — пустая клетка
        new Color(0, 240, 240),      // 1 — I голубой
        new Color(240, 240, 0),      // 2 — O жёлтый
        new Color(160, 0, 240),      // 3 — T фиолетовый
        new Color(0, 0, 240),        // 4 — J синий
        new Color(240, 160, 0),      // 5 — L оранжевый
        new Color(0, 240, 0),        // 6 — S зелёный
        new Color(240, 0, 0),        // 7 — Z красный
    };

    // ---------- СОСТОЯНИЕ ИГРЫ ----------
    int[][] field = new int[ROWS][COLS]; // поле: 0 — пусто, 1..7 — цвет лежащей клетки
    int[][] shape;                       // падающая фигура
    int color;                           // её цвет
    int x, y;                            // её позиция (левый верхний угол) в клетках
    int score = 0;
    boolean gameOver = false;
    Random random = new Random();

    Tetris() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);              // чтобы панель получала нажатия клавиш
        addKeyListener(this);            // нажатия клавиш обрабатывает этот же класс
        newFigure();

        // Таймер: каждые FALL_DELAY миллисекунд вызывает actionPerformed
        Timer timer = new Timer(FALL_DELAY, this);
        timer.start();
    }

    // ---------- ЛОГИКА ИГРЫ ----------
    /** Выбирает случайную фигуру и ставит её сверху по центру. */
    void newFigure() {
        int number = random.nextInt(SHAPES.length);
        shape = SHAPES[number];
        color = number + 1;
        x = COLS / 2 - shape[0].length / 2;   // по центру поля
        y = 0;                                // самый верх
    }

    /** Поворачивает фигуру на 90 градусов по часовой стрелке.
     *  Новая строка — это старый столбец, прочитанный снизу вверх. */
    int[][] rotate(int[][] shape) {
        int rows = shape.length;
        int cols = shape[0].length;
        int[][] result = new int[cols][rows];
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                result[col][rows - 1 - row] = shape[row][col];
            }
        }
        return result;
    }

    /** Проверяет, можно ли поставить фигуру в позицию (px, py):
     *  она не вылезает за края и не задевает лежащие клетки. */
    boolean fits(int[][] shape, int px, int py) {
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 0) continue;   // пустую клетку фигуры не проверяем
                int fx = px + col;                     // координаты этой клетки на поле
                int fy = py + row;
                if (fx < 0 || fx >= COLS || fy >= ROWS) return false; // вышли за край
                if (field[fy][fx] != 0) return false;                 // там уже что-то лежит
            }
        }
        return true;
    }

    /** Записывает фигуру в поле — теперь она лежит и больше не двигается. */
    void place() {
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    field[y + row][x + col] = color;
                }
            }
        }
    }

    /** Проверяет, заполнена ли строка целиком. */
    boolean isFull(int row) {
        for (int col = 0; col < COLS; col++) {
            if (field[row][col] == 0) return false;
        }
        return true;
    }

    /** Убирает полностью заполненные строки. Возвращает, сколько убрали. */
    int clearLines() {
        int removed = 0;
        for (int row = 0; row < ROWS; row++) {
            if (isFull(row)) {
                // все строки выше сдвигаем на одну вниз
                for (int r = row; r > 0; r--) {
                    field[r] = field[r - 1];
                }
                field[0] = new int[COLS];   // сверху появляется пустая строка
                removed++;
            }
        }
        return removed;
    }

    /** Один шаг падения. Таймер вызывает его каждые FALL_DELAY миллисекунд. */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;
        if (fits(shape, x, y + 1)) {
            y++;
        } else {
            // падать некуда — фигура ложится
            place();
            score += clearLines() * 100;
            newFigure();
            if (!fits(shape, x, y)) {
                gameOver = true;            // новой фигуре нет места
            }
        }
        repaint();                          // просим перерисовать кадр
    }

    // ---------- КЛАВИШИ ----------
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT && fits(shape, x - 1, y)) {
            x--;
        } else if (key == KeyEvent.VK_RIGHT && fits(shape, x + 1, y)) {
            x++;
        } else if (key == KeyEvent.VK_DOWN && fits(shape, x, y + 1)) {
            y++;
        } else if (key == KeyEvent.VK_UP) {
            int[][] turned = rotate(shape);
            if (fits(turned, x, y)) {
                shape = turned;
            }
        } else if (key == KeyEvent.VK_SPACE) {
            while (fits(shape, x, y + 1)) {
                y++;
            }
            actionPerformed(null);          // чтобы фигура легла сразу
        }
        repaint();
    }

    // Эти два метода обязательны для KeyListener, но нам они не нужны
    @Override
    public void keyReleased(KeyEvent e) { }

    @Override
    public void keyTyped(KeyEvent e) { }

    // ---------- РИСОВАНИЕ ----------
    /** Рисует одну клетку с тонкой серой рамкой. */
    void drawCell(Graphics g, int cx, int cy, Color c) {
        g.setColor(c);
        g.fillRect(cx * CELL, cy * CELL, CELL, CELL);
        g.setColor(new Color(40, 40, 40));
        g.drawRect(cx * CELL, cy * CELL, CELL, CELL);
    }

    /** Рисует весь кадр: поле, падающую фигуру и счёт. Java вызывает его сама. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);            // очищает панель

        // клетки, которые уже лежат
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                drawCell(g, col, row, COLORS[field[row][col]]);
            }
        }

        // падающая фигура
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    drawCell(g, x + col, y + row, COLORS[color]);
                }
            }
        }

        // счёт
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Счёт: " + score, 10, 25);

        if (gameOver) {
            g.setColor(new Color(255, 80, 80));
            g.setFont(new Font("Arial", Font.BOLD, 28));
            String text = "Игра окончена";
            int textWidth = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (WIDTH - textWidth) / 2, HEIGHT / 2);
        }
    }

    // ---------- ЗАПУСК ----------
    public static void main(String[] args) {
        JFrame window = new JFrame("Тетрис");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Tetris game = new Tetris();
        window.add(game);
        window.pack();                        // окно подгоняется под размер панели
        window.setLocationRelativeTo(null);   // по центру экрана
        window.setResizable(false);
        window.setVisible(true);
        game.requestFocusInWindow();          // клавиши идут в игру
    }
}
