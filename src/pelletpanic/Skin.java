
package pelletpanic;


public class Skin {
    private final String name;
    private final int price;
    private final boolean isPurchased;
    private final String up1Image;
    private final String down1Image;
    private final String left1Image;
    private final String right1Image;
    private final String up2Image;
    private final String down2Image;
    private final String left2Image;
    private final String right2Image;

    public Skin(String name, int price, boolean isPurchased, String up1Image, String down1Image, String left1Image, 
            String right1Image,String up2Image, String down2Image, String left2Image, String right2Image) {
        this.name = name;
        this.price = price;
        this.isPurchased = isPurchased;
        this.up1Image = up1Image;
        this.down1Image = down1Image;
        this.left1Image = left1Image;
        this.right1Image = right1Image;
        this.up2Image = up2Image;
        this.down2Image = down2Image;
        this.left2Image = left2Image;
        this.right2Image = right2Image;
    }
    
    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public String getUp1ImagePath() {
        return up1Image;
    }

    public String getDown1ImagePath() {
        return down1Image;
    }

    public String getLeft1ImagePath() {
        return left1Image;
    }

    public String getRight1ImagePath() {
        return right1Image;
    }
    public String getUp2ImagePath() {
        return up2Image;
    }

    public String getDown2ImagePath() {
        return down2Image;
    }

    public String getLeft2ImagePath() {
        return left2Image;
    }

    public String getRight2ImagePath() {
        return right2Image;
    }
    
}

