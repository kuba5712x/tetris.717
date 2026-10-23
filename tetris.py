"""Тетрис на pygame. Запуск: python3 tetris.py

Управление:
  стрелки влево/вправо — двигать фигуру
  стрелка вверх        — повернуть
  стрелка вниз         — опустить на клетку быстрее
  пробел               — уронить сразу до самого низа
"""
import random
import pygame

# ---------- НАСТРОЙКИ ----------
CELL = 30               # размер одной клетки в пикселях
COLS = 10               # сколько клеток по ширине
ROWS = 20               # сколько клеток по высоте
WIDTH = CELL * COLS     # ширина окна
HEIGHT = CELL * ROWS    # высота окна
FALL_DELAY = 500        # через сколько миллисекунд фигура падает на клетку ниже

# Семь классических фигур. 1 — клетка есть, 0 — пусто.
SHAPES = [
    [[1, 1, 1, 1]],      # I — палка

    [[1, 1],
     [1, 1]],            # O — квадрат

    [[0, 1, 0],
     [1, 1, 1]],         # T

    [[1, 0, 0],
     [1, 1, 1]],         # J

    [[0, 0, 1],
     [1, 1, 1]],         # L

    [[0, 1, 1],
     [1, 1, 0]],         # S

    [[1, 1, 0],
     [0, 1, 1]],         # Z
]

# Цвета (красный, зелёный, синий). Номер цвета = номер фигуры + 1,
# потому что 0 в поле означает "пусто".
COLORS = [
    (0, 0, 0),          # 0 — пустая клетка, чёрная
    (0, 240, 240),      # 1 — I голубой
    (240, 240, 0),      # 2 — O жёлтый
    (160, 0, 240),      # 3 — T фиолетовый
    (0, 0, 240),        # 4 — J синий
    (240, 160, 0),      # 5 — L оранжевый
    (0, 240, 0),        # 6 — S зелёный
    (240, 0, 0),        # 7 — Z красный
]


# ---------- ЛОГИКА ИГРЫ ----------
def new_figure():
    """Выбирает случайную фигуру и ставит её сверху по центру."""
    number = random.randint(0, len(SHAPES) - 1)
    shape = SHAPES[number]
    color = number + 1
    x = COLS // 2 - len(shape[0]) // 2   # по центру поля
    y = 0                                # самый верх
    return shape, color, x, y


def rotate(shape):
    """Поворачивает фигуру на 90 градусов по часовой стрелке.
    Новая строка — это старый столбец, прочитанный снизу вверх."""
    rows = len(shape)
    cols = len(shape[0])
    new_shape = []
    for col in range(cols):
        new_row = []
        for row in range(rows - 1, -1, -1):
            new_row.append(shape[row][col])
        new_shape.append(new_row)
    return new_shape


def fits(field, shape, x, y):
    """Проверяет, можно ли поставить фигуру в позицию (x, y):
    она не вылезает за края и не задевает лежащие клетки."""
    for row in range(len(shape)):
        for col in range(len(shape[row])):
            if shape[row][col] == 0:
                continue                 # пустую клетку фигуры не проверяем
            fx = x + col                 # координаты этой клетки на поле
            fy = y + row
            if fx < 0 or fx >= COLS or fy >= ROWS:
                return False             # вышли за край
            if field[fy][fx] != 0:
                return False             # там уже что-то лежит
    return True


def place(field, shape, x, y, color):
    """Записывает фигуру в поле — теперь она лежит и больше не двигается."""
    for row in range(len(shape)):
        for col in range(len(shape[row])):
            if shape[row][col] == 1:
                field[y + row][x + col] = color


def clear_lines(field):
    """Убирает полностью заполненные строки. Возвращает, сколько убрали."""
    removed = 0
    for row in range(ROWS):
        if 0 not in field[row]:          # в строке нет пустых клеток
            del field[row]               # убираем её
            field.insert(0, [0] * COLS)  # и добавляем пустую строку сверху
            removed += 1
    return removed


# ---------- РИСОВАНИЕ ----------
def draw_cell(screen, x, y, color):
    """Рисует одну клетку с тонкой серой рамкой."""
    rect = pygame.Rect(x * CELL, y * CELL, CELL, CELL)
    pygame.draw.rect(screen, color, rect)
    pygame.draw.rect(screen, (40, 40, 40), rect, 1)


def draw(screen, font, field, shape, x, y, color, score, game_over):
    """Рисует весь кадр: поле, падающую фигуру и счёт."""
    screen.fill((0, 0, 0))

    # клетки, которые уже лежат
    for row in range(ROWS):
        for col in range(COLS):
            draw_cell(screen, col, row, COLORS[field[row][col]])

    # падающая фигура
    for row in range(len(shape)):
        for col in range(len(shape[row])):
            if shape[row][col] == 1:
                draw_cell(screen, x + col, y + row, COLORS[color])

    # счёт
    text = font.render(f"Счёт: {score}", True, (255, 255, 255))
    screen.blit(text, (10, 10))

    if game_over:
        text = font.render("Игра окончена", True, (255, 80, 80))
        screen.blit(text, (WIDTH // 2 - text.get_width() // 2, HEIGHT // 2))

    pygame.display.flip()    # показываем нарисованный кадр


# ---------- ГЛАВНЫЙ ЦИКЛ ----------
def main():
    pygame.init()
    screen = pygame.display.set_mode((WIDTH, HEIGHT))
    pygame.display.set_caption("Тетрис")
    clock = pygame.time.Clock()
    font = pygame.font.SysFont("arial", 28)

    # поле — список строк, в каждой строке COLS нулей (всё пусто)
    field = [[0] * COLS for _ in range(ROWS)]
    shape, color, x, y = new_figure()
    score = 0
    fall_timer = 0       # копим миллисекунды до следующего падения
    game_over = False
    running = True

    while running:
        dt = clock.tick(60)   # не больше 60 кадров в секунду; dt — сколько мс прошло

        # 1. Клавиши
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            if event.type == pygame.KEYDOWN and not game_over:
                if event.key == pygame.K_LEFT and fits(field, shape, x - 1, y):
                    x -= 1
                elif event.key == pygame.K_RIGHT and fits(field, shape, x + 1, y):
                    x += 1
                elif event.key == pygame.K_DOWN and fits(field, shape, x, y + 1):
                    y += 1
                elif event.key == pygame.K_UP:
                    turned = rotate(shape)
                    if fits(field, turned, x, y):
                        shape = turned
                elif event.key == pygame.K_SPACE:
                    while fits(field, shape, x, y + 1):
                        y += 1
                    fall_timer = FALL_DELAY   # чтобы фигура легла сразу

        # 2. Фигура падает сама
        if not game_over:
            fall_timer += dt
            if fall_timer >= FALL_DELAY:
                fall_timer = 0
                if fits(field, shape, x, y + 1):
                    y += 1
                else:
                    # падать некуда — фигура ложится
                    place(field, shape, x, y, color)
                    score += clear_lines(field) * 100
                    shape, color, x, y = new_figure()
                    if not fits(field, shape, x, y):
                        game_over = True      # новой фигуре нет места

        # 3. Рисуем кадр
        draw(screen, font, field, shape, x, y, color, score, game_over)

    pygame.quit()


if __name__ == "__main__":
    main()
