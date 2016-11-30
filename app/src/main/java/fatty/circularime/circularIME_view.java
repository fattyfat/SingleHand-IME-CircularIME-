package fatty.circularime;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/**
 * Created by Fatty on 2016-11-29.
 */
//============最急件→製作中
//todo 右手模式
//todo 自動化測試
//============待完成
//todo 雙手模式(還需要設定橫屏，雙手模式可能需要normal模式和9宮格模式) (下面參數只是暫時設定，屆時實作要改掉)
//todo 加入數字與標點符號鍵盤

//todo 加入安裝新手設定畫面
//todo 加入廣告
//todo 加聲音或震動  (查SEE程式的playclick)
//todo 加入輸入法頁面的一些選項(給我們評分 震動勾選 聲音勾選 語言 一鍵備份等)

//todo 加入中英日切換的按鈕
//todo 加入中文輸入法
//todo 加入日文輸入法
//============觀察中
//todo 考慮是否用的到Action_MOVE
//todo 像扇子一樣的收放動作

public class circularIME_view extends View {

    fatty.circularime.circularIME circularIME;

    private float		firstCircleRadius = 0;				// Radius of circle
    private float       secendCircleRadius = 0;				// Radius of circle
    private float       thirdCircleRadius = 0;				// Radius of circle
    private float       forthCircleRadius = 0;				// Radius of circle
    private float		fifthCircleRadius = 0;				// Radius of circle
    private PointF      posTouchDown = null;				// Current point of touch
    private PointF		posCircleCenter = null;				// Current point of touch
    private String		outputString = "";			        // output string
    private String		lastTimeOutputString = "";			// last time output string
    private boolean		outOfCircle = false;			    // true = 在最外圈的外面 ; false = 正常使用輸入法的區域
    private boolean		innerOfUppercase = false;			// true = 在最內圈的圈內 ; false = 正常使用輸入法的區域
    private boolean		flag_uppercase = false;			    // true = uppercase ; false = lowercase
    private boolean		longPressEvent = false;			    // true = longPress ; false = normal case

    private static final double  amountRow = 0.0175;        // pi/180 求角度1度時的弧度
    private static final long   clickSpacingTime = 300;     // 連續點擊的間隔時間 (按鍵之間間隔小於300ms代表連續點籍)
    private static final long   longPressTime = 500;        // 超過500ms判斷為長按
    private static final long   longPressSpacingTime = 75;  // 判斷為長按之後，每隔75ms執行一次長按事件
    private static long         preClickTime = 0;           // 當前按鍵的時間
    private int                 sameButtonClickCount = 0;   // 統計連續點擊同按鈕次數的counter (間格在一個clickSpacingTime內判斷為連擊)

    private Handler mBaseHandler = new Handler();
    private LongPressedThread mLongPressedThread;           //長按線程；進入長按線程將會不斷output最近印出的字
    private ClickPressedThread mPrevClickThread;            //點擊等待線程；進入點擊線程

    private String[][]	EN_keyboard = {
            {""   , "wxyz", "tuv" , "pqrs", "63"},
            {"1#" , "mno" , "jkl" , "ghi" , "44"},
            {"33" , "def" , "abc" , "-1"  , "46"},
            {"-5" , "32"  , "-4"  , ""    , ""}};
/*          ↑上方矩陣視覺化↓ 上下顛倒左右相反，不能直接按照[row][col]這樣直接一對一對應

            ｜ENTER｜空白鍵 ｜DEL  ｜      ｜      ｜
            ｜  .  ｜大小寫 ｜ABC  ｜DEF   ｜!     ｜
            ｜  ,  ｜GHI   ｜JKL  ｜MNO   ｜12#   ｜
            ｜  ?  ｜PQRS  ｜TUV  ｜WXYZ  ｜      ｜

            註1：12# 意思為 切數字/標點符號鍵盤。
            註2：沒有內容代表目前尚未設定功能。
*/

    public circularIME_view(Context context, AttributeSet attrs) {super(context,attrs); }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){  //繪製初始畫面

        int measureWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight = View.MeasureSpec.getSize(heightMeasureSpec);

        int width = measureWidth;
        int height = measureHeight;

        if(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE){ //判斷是否為橫屏

            //todo 轉為雙手模式 下面參數只是暫時設定，屆時實作要改掉
            width = Math.round(0.5f * width);
            height = Math.round(0.75f * width);
            posCircleCenter = new PointF(0 , 0);                // Get center point 訂橫屏的左下角
        }
        else{  // Circular mode

            height = Math.round(0.75f * width);                 // 保持長寬比 4：3。原始像素800x600
            posCircleCenter = new PointF(0 , measureHeight);    // Get center point 訂直屏左下角
        }

        firstCircleRadius = Math.round(0.3f * height);
        secendCircleRadius = Math.round(0.5f * height);
        thirdCircleRadius = Math.round(0.7f * height);
        forthCircleRadius = Math.round(0.9f * height);
        fifthCircleRadius = height;

        // Set the new size
        setMeasuredDimension(width, height);
    }

    public boolean onTouchEvent(MotionEvent e){

        switch(e.getAction())
        {
            case MotionEvent.ACTION_DOWN:

                calTouchDownDirectionAndAngle(e);

                if (innerOfUppercase == false && outOfCircle == false) {  //在輸入法的區域外，不處理長按事件
                    //處理長按事件
                    mLongPressedThread = new LongPressedThread();
                    mBaseHandler.postDelayed(mLongPressedThread, longPressTime);
                }

                if(mPrevClickThread != null) mBaseHandler.removeCallbacks(mPrevClickThread);    //若有多次點擊事件的thread，這裡取消

                break;

            case MotionEvent.ACTION_MOVE:

                //todo 考慮是否用的到Action_MOVE
                break;

            case MotionEvent.ACTION_UP:

                preClickTime = Calendar.getInstance().getTimeInMillis();    //抓取手放開的時間點(判斷多次點擊事件用)

                if (mLongPressedThread!= null) mBaseHandler.removeCallbacks(mLongPressedThread);    //取消註冊的長按事件

                longPressEvent = false;

                if (outputString.equals(lastTimeOutputString) && (outputString.length() == 3 || outputString.length() == 4)) {
                    //判斷連擊
                    mPrevClickThread = new ClickPressedThread();
                    mBaseHandler.postDelayed(mPrevClickThread, clickSpacingTime);

                    sameButtonClickCount++;
                }

                lastTimeOutputString = outputString;
                posTouchDown = null;

                break;

            default:
                return false;
        }
        return true;
    }

    public class LongPressedThread implements Runnable{

        @Override
        public void run() {

            //這裡處理長按事件
            longPressEvent = true;
            printOutputString(outputString);

            //無窮new長按事件來印出字元 (每longPressSpacingTime時間執行一次)
            mLongPressedThread = new LongPressedThread();
            mBaseHandler.postDelayed(mLongPressedThread, longPressSpacingTime);
        }
    }

    public class ClickPressedThread implements Runnable{

        @Override
        public void run() {
            //進來代表間格超過0.3s，連擊中斷，counter從0開始
            sameButtonClickCount = 0;
        }
    }

    private void calTouchDownDirectionAndAngle(MotionEvent e){

        // Get touch point
        posTouchDown = new PointF(e.getX(), e.getY());

        double touchRadius = getRadius(posTouchDown);
        double touchAngle = getAngle(posTouchDown);

        if (touchRadius > fifthCircleRadius ) { //點擊座標半徑超過圖形，視為無效操作

            //do nothing
        }
        else if(touchRadius < firstCircleRadius) {

            // TODO: 之後要加入中英日切換的地方
        } else {

            //todo 如果要加聲音或震動  查SEE程式的playclick
            outputString = getButtoned(touchRadius,touchAngle);

            if (outputString != "") {

                printOutputString(outputString);
            }
        }
    }

    private double getRadius(PointF p)
    {
        // Get difference of coordinates
        double x = p.x - posCircleCenter.x;
        double y = p.y - posCircleCenter.y;

        // Return distance calculated with Pythagoras
        return Math.sqrt(x*x + y*y);
    }

    /** Gets the angle of point p relative to the center 弧度*/
    private double getAngle(PointF p)
    {
        // Get difference of coordinates
        double x = p.x - posCircleCenter.x;
        double y = p.y - posCircleCenter.y;

        // Calculate angle with special atan (calculates the correct angle in all quadrants)
        double angle = Math.atan2(y, x);

        return Math.abs(angle);
    }

    private String getButtoned(double Radius, double angle)
    {

        int whichCycle = 0;
        int whichDirection = 0;

        //找這座標在第幾圈
        //以圓的1/10作為除數，計算半徑有十分之一半徑的幾倍來計算在哪一圈
        //小於3 第一圈(最內圈) 中英日切換輸入法，但由於在input_onTouchUp內已經篩選過，所以這裡不會出現
        //3<= X <5 第一圈  (最內圈)
        //>=5 X <7 第二圈
        //>=7 X <9 第三圈
        //>=9 X <10 第四圈 (最外圈)
        int radiusMultiple = (int)((float)Radius/(fifthCircleRadius/10));

        if (radiusMultiple >= 9)
            whichCycle = 3;
        else if (radiusMultiple >= 7)
            whichCycle = 2;
        else if (radiusMultiple >= 5)
            whichCycle = 1;
        else if (radiusMultiple >= 3)
            whichCycle = 0;

        int angleMultiple = (int) (angle / amountRow);              //找這角度在哪一個column

        if(whichCycle == 3) {                                       //如果在最外圈

            if(angleMultiple <= 30) whichDirection = 0;             //小於等於30度，設whichDirection為0。 (最外圈DEL鍵)
            else if (angleMultiple <= 60) whichDirection = 1;       //大於30，小於等於60度，設whichDirection為1。 (最外圈空白鍵)
            else whichDirection = 2;                                //大於60，小魚等於90度，設whichDirection為2。 (最外圈ENTER鍵)

        } else{                                                     //內圈4圈

            if(angleMultiple <= 15) whichDirection = 0;             //小於等於15度，設whichDirection為0。(NULL；12#)
            else if (angleMultiple <= 35) whichDirection = 1;       //大於15，小於等於35度，設whichDirection為1。(WXYZ；MNO；DEF)
            else if (angleMultiple <= 55) whichDirection = 2;       //大於35，小於等於55度，設whichDirection為2。(TUV；JKL；ABC)
            else if (angleMultiple <= 75) whichDirection = 3;       //大於55，小於等於75度，設whichDirection為3。(PQRS；GHI；↑)
            else whichDirection = 4;                                //大於75，小於等於90度，設whichDirection為4。(?；,；.)
        }

        return EN_keyboard[whichCycle][whichDirection];
    }

    private void printOutputString(String outputString_)
    {

        if (outputString_.length() == 3 || outputString_.length() == 4) {   //九宮格鍵盤，字元輸出鍵盤有3或4個字

            if (longPressEvent) {
                ;   //長按事件 do nothing  直接跳到下面印出字元
            }else if (outputString.equals(lastTimeOutputString) && (Calendar.getInstance().getTimeInMillis() - preClickTime <= clickSpacingTime))
                circularIME.delOneText();   //若文字鍵與上一次按鍵是同一顆，且時間差距小於0.3秒，則做一次刪除動作(因為九宮格要切換字元)
            else sameButtonClickCount = 0;  //代表這次按鍵與上次不同顆，所以歸零

            if(flag_uppercase) circularIME.outputText("" + Character.toUpperCase(outputString_.charAt(sameButtonClickCount %outputString_.length()))); //大寫
            else circularIME.outputText("" + outputString_.charAt(sameButtonClickCount %outputString_.length()));
        }
        else switch (outputString_) //特殊符號走這邊
        {
            case "-4":
                circularIME.outputKeyEvent(KeyEvent.KEYCODE_ENTER);//Enter鍵
                break;

            case "-5":
                circularIME.outputKeyEvent(KeyEvent.KEYCODE_DEL);//DEL鍵
                break;

            case "-1":
                flag_uppercase = !flag_uppercase;
                handleBackground();
                break;

            case "1#":
                //TODO 加入數字與標點符號鍵盤
                break;

            default:
                //其餘特殊符號
                circularIME.outputText( String.valueOf((char) Integer.parseInt(outputString_)));
        }
    }

    /** 英文大小寫背景圖變換設定 */
    private void handleBackground()
    {
        if(flag_uppercase)
        {
            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_circular_5x3_ime_background_en_uppercase);
        }
        else
        {
            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_circular_5x3_ime_background_en_lowercase);
        }
    }

    public void setIME(fatty.circularime.circularIME _circularIME) {circularIME = _circularIME; }
}
