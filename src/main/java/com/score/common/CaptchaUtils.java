package com.score.common;

import lombok.Data;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * @author imyiwen
 * @data 2026/4/29 9:41
 */
public class CaptchaUtils {
    private static final int WIDTH=320;
    private static final int HEIGHT=160;
    //拼图边长
    private static final int L=42;
    //拼图凸起半径
    private static final int R=10;

    public static void createCaptcha(CaptchaResult result) throws Exception{
        //生成随机背景图
        BufferedImage backImg =new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = backImg.createGraphics();
        Random random=new Random();

        Color c1 = new Color(random.nextInt(150)+50,random.nextInt(150)+50,random.nextInt(150));
        Color c2 = new Color(random.nextInt(150)+50,random.nextInt(150)+50,random.nextInt(150));
        GradientPaint gp =new GradientPaint(0, 0, c1, 0, HEIGHT, c2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        //绘制噪点 干扰线
        for(int i=0;i<20;i++){
            g2d.setColor(new Color(255,255,255,40));
            g2d.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(50), random.nextInt(50));
        }
        //生成拼图位置
        int maxX=WIDTH-L-(int)(R*1.5)-10;
        int minX =WIDTH/2;
        int x =random.nextInt(maxX-minX)+minX;
        int maxY=HEIGHT-L-10;
        int minY=(int)(R*1.5)+10;
        int y =random.nextInt(maxY-minY)+minY;

        //创建滑块图
        BufferedImage sliderImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sliderImg.createGraphics();
        Shape shapeAtZero = getPuzzleShape(0, y);
        g.setClip(shapeAtZero);
        g.drawImage(backImg,-x,0,null);

        g.setClip(null);
        g.setColor(new Color(255,255,255,200));
        g.setStroke(new BasicStroke(2));
        g.draw(shapeAtZero);
        g.dispose();

        Shape shapeAtX=getPuzzleShape(x, y);
        g2d.setColor(new Color(0,0,0,150));
        g2d.fill(shapeAtX);
        g2d.dispose();

        result.setX(x);
        result.setBackBase64(toBase64(backImg,"jpg"));
        result.setSliderBase64(toBase64(sliderImg,"png"));
    }

    private static Shape getPuzzleShape(int x,int y){
        Path2D path = new Path2D.Double();
        path.moveTo(x,y);
        path.lineTo(x+L/3.0,y);
        path.quadTo(x + L / 2.0, y - R * 1.5, x + 2.0 * L / 3.0, y);
        path.lineTo(x + L, y);
        path.lineTo(x + L, y + L / 3.0);
        path.quadTo(x + L + R * 1.5, y + L / 2.0, x + L, y + 2.0 * L / 3.0);
        path.lineTo(x + L, y + L);
        path.lineTo(x, y + L);
        path.lineTo(x, y);
        path.closePath();
        return path;
    }

    private static String toBase64(BufferedImage image,String type) throws Exception {
        ByteArrayOutputStream os =new ByteArrayOutputStream();
        ImageIO.write(image,type,os);
        return "data:image/"+type+";base64,"+java.util.Base64.getEncoder().encodeToString(os.toByteArray());
    }

    @Data
    public static class CaptchaResult{
        private int x;
        private String backBase64;
        private String sliderBase64;
    }
}
