package cirilo.atc.service;

// Uma classe interna simples para manter os resultados do OCR localmente
// antes de mapeá-los para ApiBalloon.
public class OcrTextRegion {
    String text;
    int x, y, width, height;

    public OcrTextRegion(String text, int x, int y, int width, int height) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getText() { return text; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
