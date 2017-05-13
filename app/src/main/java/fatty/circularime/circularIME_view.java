package fatty.circularime;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/**
 * Created by Fatty on 2016-11-29.
 */

public class circularIME_view extends View {

    fatty.circularime.circularIME circularIME;
    fatty.circularime.en_circularIME_view_left en_circularIME_view_left = new fatty.circularime.en_circularIME_view_left();
    fatty.circularime.en_circularIME_view_right en_circularIME_view_right = new fatty.circularime.en_circularIME_view_right();
    fatty.circularime.en_circularIME_view_twoHands en_circularIME_view_twoHands = new fatty.circularime.en_circularIME_view_twoHands();

    private int         IMEclassNumber = 0;         // 0 = en_circularIME_left ; 1 = en_circularIME_view_twoHands ;
                                                    // 2 = en_circularIME_right
    private boolean     isHorizontal = false;       // true = Horizontal ; false = vertical
    private int         screenWidth,screenHeight,view_width,view_height;

    private PointF      posTouchDown = null;        // Current point of touch
    private boolean     flag_uppercase = false;     // true = uppercase ; false = lowercase
    private boolean     longPressEvent = false;     // true = longPress ; false = normal case
    private final long  clickSpacingTime = 300;     // 連續點擊的間隔時間 (按鍵之間間隔小於300ms代表連續點擊)
    private final long  longPressTime = 500;        // 超過500ms判斷為長按
    private final long  longPressSpacingTime = 75;  // 判斷為長按之後，每隔75ms執行一次長按事件
    private long        preClickTime = 0;           // 當前按鍵的時間
    private int         sameButtonClickCount = 0;   // 統計連續點擊同按鈕次數的counter (間隔在一個clickSpacingTime內判斷為連擊)
    private String	    outputString = "";          // output string
    private String	    lastTimeOutputString;	    // last time output string

    private Handler mBaseHandler = new Handler();
    private LongPressedThread mLongPressedThread;   //長按線程；進入長按線程將會不斷output最近印出的字
    private ClickPressedThread mPrevClickThread;    //點擊等待線程；進入點擊線程

    private Context mContext;
    private boolean adFlag = false;
    private boolean enableAD = false;
    private String SceneNumber = "";

    private Bitmap adClickIcon, switchIcon;
    private TextPaint paint;

    public circularIME_view(Context context, AttributeSet attrs) {

        super(context,attrs);
        mContext = context;

        //创建一个画笔
        paint = new TextPaint();

        String familyName = "Arimo";
        Typeface font = Typeface.create(familyName,Typeface.NORMAL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(font);

        //获得图片资源
        switchIcon = BitmapFactory.decodeResource(getResources(), R.drawable.switch_pic);
        adClickIcon = BitmapFactory.decodeResource(getResources(), R.drawable.dog_and_scroll);
    }

    public void setIME(fatty.circularime.circularIME _circularIME) {circularIME = _circularIME; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){  //繪製初始畫面

        screenWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        screenHeight = View.MeasureSpec.getSize(heightMeasureSpec);

        view_width = screenWidth;
        view_height = screenHeight;

        if(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE){ //判斷是否為橫屏

            isHorizontal = true;
            view_height = Math.round(0.25f * view_width);
            en_circularIME_view_twoHands.setCircularRadius_enTwoHand(screenWidth,screenHeight,view_width,view_height,true);
            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_horizontal_5x3_ime_background_lowercase);
            flag_uppercase = false;
            IMEclassNumber = 1;
        }
        else{  // Circular mode
            isHorizontal = false;
            view_height = Math.round(0.75f * view_width);                 // 保持長寬比 4：3。原始像素800x600
            en_circularIME_view_left.setCircularRadius_enLeft(screenWidth,screenHeight,view_width,view_height);

            switch((int)view_width/300) {

                case 0:
                    paint.setTextSize(12);
                    break;
                case 1:
                    paint.setTextSize(18);
                    break;
                case 2:
                    paint.setTextSize(29);
                    break;
                case 3:
                    paint.setTextSize(40);
                    break;
                default:
                    paint.setTextSize(50);
                    break;
            }
        }
        // Set the new size
        setMeasuredDimension(view_width, view_height);
    }

    public boolean onTouchEvent(MotionEvent e){

        switch(e.getAction())
        {
            case MotionEvent.ACTION_DOWN:

                if(mPrevClickThread != null) mBaseHandler.removeCallbacks(mPrevClickThread);    //若有多次點擊事件的thread，這裡取消

                // Get touch point
                posTouchDown = new PointF(e.getX(), e.getY());

                String returnString = "";
                returnString = IMEclassSwitch();

                if(returnString.length() != 0) {
                    switch (returnString.substring(0,2)) {
                        case "#1":     //do nothing(在外圈無用地帶)
                            break;

                        case "#2":     //切換左右雙手輸入法
                            IMEkeyboardBackgroundSwitch("switchToOtherIME");
                            break;

                        case "#3":     //切換英中日輸入法
                            // TODO: 之後要加入中英日切換的地方
                            break;

                        case "#4":     //廣告對話事件或廣告回答NO
                            SceneNumber = returnString;
                            invalidate();   //重新進入ondraw()
                            break;

                        case "#5":     //廣告回答YES

                            adFlag = true;

                            Intent intent = new Intent();
                            intent.setClass(getContext(), AD_view.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                            mContext.startActivity(intent);

                            break;

                        default:        //output string
                            // TODO 如果要加聲音或震動  查SEE程式的playclick
                            outputString = returnString;

                            if (!outputString.equals("")) printOutputString(outputString);
                            lastTimeOutputString = outputString;

                            //加入長按事件thread
                            mLongPressedThread = new LongPressedThread();
                            mBaseHandler.postDelayed(mLongPressedThread, longPressTime);
                            break;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:

                preClickTime = Calendar.getInstance().getTimeInMillis();                        //抓取手放開的時間點(判斷多次點擊事件用)
                if (mLongPressedThread!= null) mBaseHandler.removeCallbacks(mLongPressedThread);//取消註冊的長按事件

                if (outputString.equals(lastTimeOutputString) && (outputString.length() == 3 || outputString.length() == 4)) {
                    //判斷連擊
                    mPrevClickThread = new ClickPressedThread();
                    mBaseHandler.postDelayed(mPrevClickThread, clickSpacingTime);

                    sameButtonClickCount++;
                }

                longPressEvent = false;
                posTouchDown = null;

                break;

            default:
                return false;
        }
        return true;
    }   /**偵測碰觸事件(action down & action up)*/

    protected void onDraw (Canvas canvas) {

        super.onDraw(canvas);

        if (!isHorizontal) {    //橫版的話不印廣告與切換鍵盤的符號
            adFlag = !adFlag;
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            switch (IMEclassNumber % 3) {
                case 0:     //變更為左手鍵盤廣告圖案位置

                    //if ((hour == day%12) || (hour == (day%12+12))) {

                        canvas.scale(0.2f, 0.2f);
                        canvas.drawBitmap(adClickIcon, screenWidth * 4.3f, screenHeight * 0.2f, paint);
                        canvas.scale(5, 5);
                        enableAD = true;

                        if (SceneNumber.equals("#4")) { //若為#4(廣告)，則印出YES NO對話字串

                            Rect rect;
                            String text;
                            StaticLayout layout;

                            if(adFlag) {
                                rect = new Rect((int)(screenWidth*0.6f), (int)(screenHeight*0.17f), screenWidth, (int)(screenHeight*0.3f));    //印出文字訊息
                                text = this.getResources().getString(R.string.adContentText);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.LEFT);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();

                                rect = new Rect((int)(screenWidth*0.65f), (int)(screenHeight*0.35f), (int)(screenWidth*0.8f), (int)(screenHeight*0.45f));    //印出YES圖案
                                text = this.getResources().getString(R.string.adContentOptionAgree);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.LEFT);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();

                                rect = new Rect((int)(screenWidth*0.85f), (int)(screenHeight*0.35f), screenWidth, (int)(screenHeight*0.45f));    //印出NO圖案
                                text = this.getResources().getString(R.string.adContentOptionCancel);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.CENTER);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();
                            }
                        }
                    //}else{
                        //enableAD = false;
                    //}

                    canvas.scale(0.3f, 0.3f);
                    canvas.drawBitmap(switchIcon, screenWidth*2.92f, screenHeight*2.92f, paint);

                    SceneNumber = "";
                    break;
                case 1:     //變更為雙手鍵盤廣告圖案位置

                    if ((hour == day%12) || (hour == (day%12+12))) {

                        canvas.scale(0.2f, 0.2f);
                        canvas.drawBitmap(adClickIcon, screenWidth*0.1f, screenHeight*4.1f, paint);
                        canvas.scale(5, 5);
                        enableAD = true;

                        if (SceneNumber.equals("#4")) { //若為#4(廣告)，則印出YES NO對話字串

                            Rect rect;
                            String text;
                            StaticLayout layout;

                            if(adFlag) {

                                rect = new Rect((int)(screenWidth*0.2f), (int)(screenHeight*0.81f), (int)(screenWidth*0.6f), (int)(screenHeight));    //印出文字訊息
                                text = this.getResources().getString(R.string.adContentText);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.LEFT);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();

                                rect = new Rect((int)(screenWidth*0.6f), (int)(screenHeight*0.9f), (int)(screenWidth*0.75f), (int)(screenHeight));    //印出YES圖案
                                text = this.getResources().getString(R.string.adContentOptionAgree);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.LEFT);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();

                                rect = new Rect((int)(screenWidth*0.75f), (int)(screenHeight*0.9f), (int)(screenWidth*0.9f), (int)(screenHeight));    //印出NO圖案
                                text = this.getResources().getString(R.string.adContentOptionCancel);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.CENTER);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();
                            }
                        }
                    }else{
                            enableAD = false;
                    }

                    canvas.scale(0.3f, 0.3f);
                    canvas.drawBitmap(switchIcon, screenWidth*2.92f, screenHeight*2.92f, paint);

                    SceneNumber = "";
                    break;
                case 2:     //變更為右手鍵盤廣告圖案位置

                    if ((hour == day%12) || (hour == (day%12+12))) {

                        canvas.scale(0.2f, 0.2f);
                        canvas.drawBitmap(adClickIcon, screenWidth*0.1f, screenHeight*0.2f, paint);
                        canvas.scale(5, 5);
                        enableAD = true;

                        if (SceneNumber.equals("#4")) { //若為#4(廣告)，則印出YES NO對話字串

                            Rect rect;
                            String text;
                            StaticLayout layout;

                            if(adFlag) {

                                rect = new Rect((int)(screenWidth*0.02f), (int)(screenHeight*0.17f), (int)(screenWidth*0.4f), (int)(screenHeight*0.3f));    //印出文字訊息
                                text = this.getResources().getString(R.string.adContentText);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.LEFT);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();

                                rect = new Rect((int)(screenWidth*0.05f), (int)(screenHeight*0.35f), (int)(screenWidth*0.2f), (int)(screenHeight*0.45f));    //印出YES圖案
                                text = this.getResources().getString(R.string.adContentOptionAgree);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.LEFT);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();

                                rect = new Rect((int)(screenWidth*0.25f), (int)(screenHeight*0.35f), (int)(screenWidth*0.4f), (int)(screenHeight*0.45f));    //印出NO圖案
                                text = this.getResources().getString(R.string.adContentOptionCancel);
                                layout = new StaticLayout(text, paint, rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                canvas.save();
                                paint.setTextAlign(Paint.Align.CENTER);
                                canvas.translate(rect.left, rect.top);
                                layout.draw(canvas);
                                canvas.restore();
                            }
                        }
                    }else{
                        enableAD = false;
                    }

                    canvas.scale(0.3f, 0.3f);
                    canvas.drawBitmap(switchIcon, screenWidth*0.1f, screenHeight*2.92f, paint);

                    SceneNumber = "";
                    break;
                default:
                    break;
            }
        }
    }

    private class LongPressedThread implements Runnable{

        @Override
        public void run() {     //這裡處理長按事件

            longPressEvent = true;
            printOutputString(outputString);

            //無窮new長按事件來印出字元 (每longPressSpacingTime時間執行一次)
            mLongPressedThread = new LongPressedThread();
            mBaseHandler.postDelayed(mLongPressedThread, longPressSpacingTime);
        }
    }   /**判斷長按事件的thread*/

    private class ClickPressedThread implements Runnable{

        @Override
        public void run() {     //進來代表間格超過0.3s，連擊中斷，counter從0開始

            sameButtonClickCount = 0;
        }
    }   /**判斷連擊的thread*/

    private void printOutputString(String outputString_)    /**印出字*/
    {

        if (outputString_.length() == 3 || outputString_.length() == 4) {   //九宮格鍵盤，字元輸出鍵盤有3或4個字

            if (!longPressEvent) {
                if (outputString.equals(lastTimeOutputString) && (Calendar.getInstance().getTimeInMillis() - preClickTime <= clickSpacingTime)){
                    circularIME.delOneText();   //若文字鍵與上一次按鍵是同一顆，且時間差距小於0.3秒，則做一次刪除動作(因為九宮格要切換字元)
                }else sameButtonClickCount = 0;  //代表這次按鍵與上次不同顆或者連擊次數中斷，所以歸零
            }

     /*大寫*/if(flag_uppercase) circularIME.outputText("" + Character.toUpperCase(outputString_.charAt(sameButtonClickCount %outputString_.length())));
     /*小寫*/else circularIME.outputText("" + outputString_.charAt(sameButtonClickCount %outputString_.length()));
        }
        else switch (outputString_) //特殊符號走這邊
        {
            case "-4"://Enter鍵
                circularIME.outputKeyEvent(KeyEvent.KEYCODE_ENTER);
                break;

            case "-5"://DEL鍵
                circularIME.outputKeyEvent(KeyEvent.KEYCODE_DEL);
                break;

            case "-1"://大小寫切換
                flag_uppercase = !flag_uppercase;
                IMEkeyboardBackgroundSwitch("upperLowerCaseSwitch");
                break;

            case "1#"://切換數字鍵盤→切換標點符號鍵盤
                IMEkeyboardBackgroundSwitch("numeralSymbolSwitch");
                break;

            default://其餘特殊符號
                circularIME.outputText( String.valueOf((char) Integer.parseInt(outputString_)));
        }
    }

    private String IMEclassSwitch() /**根據不同的鍵盤種類，回傳要印出的字*/
    {
        switch(IMEclassNumber % 3) {
            case 0:     //左手鍵盤回傳點擊結果
                return en_circularIME_view_left.ACTION_DOWN_EVENT(posTouchDown, enableAD, adFlag);

            case 1:     //兩手鍵盤回傳點擊結果
                return en_circularIME_view_twoHands.ACTION_DOWN_EVENT(posTouchDown, enableAD, adFlag);

            case 2:     //右手鍵盤回傳點擊結果
                return en_circularIME_view_right.ACTION_DOWN_EVENT(posTouchDown, enableAD, adFlag);

            default:
                return "";
        }

    }

    private void IMEkeyboardBackgroundSwitch(String switchEvent)    /**切換鍵盤畫面*/
    {

        adFlag = true;
        switch(switchEvent) {

            case "switchToOtherIME":  /**左手輸入法←→兩手輸入法←→右手輸入法 背景圖更換*/

                IMEclassNumber++;
                SceneNumber = "";
                switch (IMEclassNumber % 3) {
                    case 0:     //變更為左手鍵盤的背景及設定

                        en_circularIME_view_left.setCircularRadius_enLeft(screenWidth, screenHeight, view_width, view_height);
                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_normal_circular_5x3_ime_background_en_lowercase);
                        flag_uppercase = false;
                        this.invalidate();
                        break;

                    case 1:     //變更為雙手鍵盤的背景及設定

                        en_circularIME_view_twoHands.setCircularRadius_enTwoHand(screenWidth, screenHeight, view_width, view_height, false);
                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_lowercase);
                        flag_uppercase = false;
                        this.invalidate();
                        break;

                    case 2:     //變更為右手鍵盤的背景及設定

                        en_circularIME_view_right.setCircularRadius_enRight(screenWidth, screenHeight, view_width, view_height);
                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_normal_circular_5x3_ime_background_en_lowercase);
                        flag_uppercase = false;
                        this.invalidate();
                        break;

                    default:
                        break;
                }
            break;

            case "upperLowerCaseSwitch": /** 英文大寫←→英文小寫 背景圖更換 */

                if (isHorizontal) {

                    if (flag_uppercase)
                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_horizontal_5x3_ime_background_uppercase);
                    else
                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_horizontal_5x3_ime_background_lowercase);
                    break;

                }else {
                    switch (IMEclassNumber % 3) {
                        case 0:     //左手鍵盤

                            if (flag_uppercase)
                                findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_normal_circular_5x3_ime_background_en_uppercase);
                            else
                                findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_normal_circular_5x3_ime_background_en_lowercase);
                            break;

                        case 1:     //雙手鍵盤

                            if (flag_uppercase)
                                findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_uppercase);
                            else
                                findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_lowercase);
                            break;

                        case 2:     //右手鍵盤

                            if (flag_uppercase)
                                findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_normal_circular_5x3_ime_background_en_uppercase);
                            else
                                findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_normal_circular_5x3_ime_background_en_lowercase);
                            break;
                    }
                }
                break;

            case "numeralSymbolSwitch": /** 數字鍵盤←→標點符號鍵盤 背景圖更換 */

                if (isHorizontal) {

                    switch (en_circularIME_view_twoHands.switchKeyboardArray()) {

                        case 0:
                            if (flag_uppercase)
                                findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_horizontal_5x3_ime_background_uppercase);
                            else
                                findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_horizontal_5x3_ime_background_lowercase);
                            break;

                        case 1:
                            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_horizontal_numeral_5x3_ime_background);
                            break;

                        case 2:
                            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_horizontal_symbol_5x3_ime_background);
                            break;

                        default:
                            break;
                    }
                }else {
                    switch (IMEclassNumber % 3) {

                        case 0://左手鍵盤
                            switch (en_circularIME_view_left.switchKeyboardArray()) {

                                case 0:
                                    if (flag_uppercase)
                                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_normal_circular_5x3_ime_background_en_uppercase);
                                    else
                                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_normal_circular_5x3_ime_background_en_lowercase);
                                    break;

                                case 1:
                                    findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_numeral_circular_5x3_ime_background);
                                    break;

                                case 2:
                                    findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_symbol_circular_5x3_ime_background);
                                    break;

                                default:
                                    break;
                            }

                            break;

                        case 1://雙手鍵盤

                            switch (en_circularIME_view_twoHands.switchKeyboardArray()) {

                                case 0:
                                    if (flag_uppercase)
                                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_uppercase);
                                    else
                                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_lowercase);
                                    break;

                                case 1:
                                    findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_numeral_5x3_ime_background);
                                    break;

                                case 2:
                                    findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_symbol_5x3_ime_background);
                                    break;

                                default:
                                    break;
                            }
                            break;

                        case 2://右手鍵盤

                            switch (en_circularIME_view_right.switchKeyboardArray()) {

                                case 0:
                                    if (flag_uppercase)
                                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_normal_circular_5x3_ime_background_en_uppercase);
                                    else
                                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_normal_circular_5x3_ime_background_en_lowercase);
                                    break;

                                case 1:
                                    findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_numeral_circular_5x3_ime_background);
                                    break;

                                case 2:
                                    findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_symbol_circular_5x3_ime_background);
                                    break;

                                default:
                                    break;
                            }
                            break;
                    }
                }
                break;
        }
    }

}