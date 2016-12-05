package fatty.circularime;

import android.graphics.PointF;

/**
 * Created by Fatty on 2016-12-02.
 */

public class en_circularIME_view_left {

    fatty.circularime.circularIME circularIME;
    fatty.circularime.circularIME_view circularIME_view;

    private PointF posCircleCenter;                                                // Left circularIME's center point of touch
    private float firstCircleRadius, secendCircleRadius, thirdCircleRadius,
            forthCircleRadius, fifthCircleRadius;                    // Radius of circle

    final double amountRow = 0.0175;           /// pi/180 角度1度時的弧度

    final private String[][] EN_keyboardArray = {
            {"", "wxyz", "tuv", "pqrs", "63"},
            {"1#", "mno", "jkl", "ghi", "44"},
            {"33", "def", "abc", "-1", "46"},
            {"-5", "32", "-4", "", ""}};
/*          ↑上方矩陣視覺化↓ 上下顛倒左右相反，不能直接按照[row][col]這樣直接一對一對應

            ｜ENTER｜空白鍵 ｜DEL  ｜      ｜      ｜
            ｜  .  ｜大小寫 ｜ABC  ｜DEF   ｜!     ｜
            ｜  ,  ｜GHI   ｜JKL  ｜MNO   ｜12#   ｜
            ｜  ?  ｜PQRS  ｜TUV  ｜WXYZ  ｜      ｜

            註1：12# 意思為 切數字/標點符號鍵盤。
            註2：沒有內容代表目前尚未設定功能。
*/

    public en_circularIME_view_left() {

        posCircleCenter = null;                /// Current point of touch

        firstCircleRadius = 0;                /// Radius of circle
        secendCircleRadius = 0;                /// Radius of circle
        thirdCircleRadius = 0;                /// Radius of circle
        forthCircleRadius = 0;                /// Radius of circle
        fifthCircleRadius = 0;                /// Radius of circle
    }

    public void setCircularRadius_enLeft(int screenWidth, int screenHeight, int view_width, int view_height) {

        firstCircleRadius = Math.round(0.3f * view_height);
        secendCircleRadius = Math.round(0.5f * view_height);
        thirdCircleRadius = Math.round(0.7f * view_height);
        forthCircleRadius = Math.round(0.9f * view_height);
        fifthCircleRadius = view_height;

        posCircleCenter = new PointF(0, screenHeight);    // Set center point 訂直屏左下角
    }

    public String ACTION_DOWN_EVENT(PointF posTouchDown){
    /**return #1:do nothing(在外圈無用地帶) #2:切換左右雙手輸入法 #3:切換英中日輸入法 #4:output String*/
        double touchRadius = getRadius(posTouchDown);
        double touchAngle = getAngle(posTouchDown);

        if (touchRadius > fifthCircleRadius ) { //點擊座標半徑超過圖形，視為無效操作

            int radiusMultiple = (int) ((float) touchRadius / firstCircleRadius);
            int angleMultiple = (int) (touchAngle / amountRow);

            if (radiusMultiple == 4 && (1 <= angleMultiple && angleMultiple <= 7))
                return "#2";

            return "#1";
        }
        else if(touchRadius < firstCircleRadius) {
            return "#3";
        } else {
            return getButtoned(posTouchDown);
        }
    }

    private double getRadius(PointF p)
    {
        // Get difference of coordinates
        double x = p.x - posCircleCenter.x;
        double y = p.y - posCircleCenter.y;

        // Return distance calculated with Pythagoras
        return Math.sqrt(x * x + y * y);
    }

    //** Gets the angle of point p relative to the center 弧度*/
    private double getAngle(PointF p)
    {
        // Get difference of coordinates
        double x = p.x - posCircleCenter.x;
        double y = p.y - posCircleCenter.y;

        // Calculate angle with special atan (calculates the correct angle in all quadrants)
        double angle = Math.atan2(y, x);

        return Math.abs(angle);
    }

    private String getButtoned(PointF posTouchDown)
    {

        double Radius = getRadius(posTouchDown);
        double Angle = getAngle(posTouchDown);

        int whichCycle = 0;
        int whichDirection = 0;

        //找這座標在第幾圈
        //以圓的1/10作為除數，計算半徑有十分之一半徑的幾倍來計算在哪一圈
        //小於3 第一圈(最內圈) 中英日切換輸入法，但由於在input_onTouchUp內已經篩選過，所以這裡不會出現
        //3<= X <5 第一圈  (最內圈)
        //>=5 X <7 第二圈
        //>=7 X <9 第三圈
        //>=9 X <10 第四圈 (最外圈)
        int radiusMultiple = (int) ((float) Radius / (fifthCircleRadius / 10));

        if (radiusMultiple >= 9)
            whichCycle = 3;
        else if (radiusMultiple >= 7)
            whichCycle = 2;
        else if (radiusMultiple >= 5)
            whichCycle = 1;
        else if (radiusMultiple >= 3)
            whichCycle = 0;

        int angleMultiple = (int) (Angle / amountRow);              //找這角度在哪一個column

        if (whichCycle == 3) {                                       //如果在最外圈

            if (angleMultiple <= 30)
                whichDirection = 0;             //小於等於30度，設whichDirection為0。 (最外圈DEL鍵)
            else if (angleMultiple <= 60)
                whichDirection = 1;       //大於30，小於等於60度，設whichDirection為1。 (最外圈空白鍵)
            else
                whichDirection = 2;                                //大於60，小魚等於90度，設whichDirection為2。 (最外圈ENTER鍵)

        } else {                                                     //內圈4圈

            if (angleMultiple <= 15)
                whichDirection = 0;             //小於等於15度，設whichDirection為0。(NULL；12#)
            else if (angleMultiple <= 35)
                whichDirection = 1;       //大於15，小於等於35度，設whichDirection為1。(WXYZ；MNO；DEF)
            else if (angleMultiple <= 55)
                whichDirection = 2;       //大於35，小於等於55度，設whichDirection為2。(TUV；JKL；ABC)
            else if (angleMultiple <= 75)
                whichDirection = 3;       //大於55，小於等於75度，設whichDirection為3。(PQRS；GHI；↑)
            else
                whichDirection = 4;                                //大於75，小於等於90度，設whichDirection為4。(?；,；.)
        }

        return EN_keyboardArray[whichCycle][whichDirection];
    }
}