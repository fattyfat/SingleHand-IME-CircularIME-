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
//todo{13 14}   加入廣告(另外弄一個廣告class可能比較好做?)
//============待完成
//todo{15~24}   調整介面+素材
//todo 小熊的家

//todo 加入中英日切換的按鈕
//todo 加入中文輸入法
//todo 加入日文輸入法
//============觀察中
//todo [考慮]自動化測試
//todo [考慮]加入安裝新手設定畫面
//todo [考慮]增加全選→(再按一次)複製→(再案一次)貼上 的按鈕?
//todo [考慮]收起鍵盤的按鈕
//todo [考慮]加聲音或震動  (查SEE程式的playclick)
//todo [考慮]加入輸入法頁面的一些選項(給我們評分 震動勾選 聲音勾選 語言 一鍵備份等)
//todo [考慮]換ime圖片用覆蓋的(用按鈕?)
//todo [考慮]像扇子一樣的收放動作

//todo [追蹤]有長按事件thread 停不下來的問題 (觸發條件不明)

public class circularIME_view extends View {

    fatty.circularime.circularIME circularIME;
    fatty.circularime.en_circularIME_view_left en_circularIME_view_left = new fatty.circularime.en_circularIME_view_left();
    fatty.circularime.en_circularIME_view_right en_circularIME_view_right = new fatty.circularime.en_circularIME_view_right();
    fatty.circularime.en_circularIME_view_twoHands en_circularIME_view_twoHands = new fatty.circularime.en_circularIME_view_twoHands();

    private int         IMEclassNumber = 0;         // 0 = en_circularIME_left ; 1 = en_circularIME_view_twoHands ;
                                                    // 2 = en_circularIME_right
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

    public circularIME_view(Context context, AttributeSet attrs) {super(context,attrs); }

    public void setIME(fatty.circularime.circularIME _circularIME) {circularIME = _circularIME; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){  //繪製初始畫面

        screenWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        screenHeight = View.MeasureSpec.getSize(heightMeasureSpec);

        view_width = screenWidth;
        view_height = screenHeight;

        if(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE){ //判斷是否為橫屏

            view_height = Math.round(0.25f * view_width);
            en_circularIME_view_twoHands.setCircularRadius_enTwoHand(screenWidth,screenHeight,view_width,view_height,true);
            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_lowercase);
            flag_uppercase = false;
            IMEclassNumber = 1;
        }
        else{  // Circular mode
            view_height = Math.round(0.75f * view_width);                 // 保持長寬比 4：3。原始像素800x600
            en_circularIME_view_left.setCircularRadius_enLeft(screenWidth,screenHeight,view_width,view_height);
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

                switch(returnString) {
                    case "#1":     //do nothing(在外圈無用地帶)
                        break;

                    case "#2":     //切換左右雙手輸入法
                        IMEkeyboardBackgroundSwitch("switchToOtherIME");
                        break;

                    case "#3":     //切換英中日輸入法
                        // TODO: 之後要加入中英日切換的地方
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

            if (longPressEvent) {
                ;   //長按事件 do nothing  直接跳到下面印出字元
            }else if (outputString.equals(lastTimeOutputString) && (Calendar.getInstance().getTimeInMillis() - preClickTime <= clickSpacingTime))
                circularIME.delOneText();   //若文字鍵與上一次按鍵是同一顆，且時間差距小於0.3秒，則做一次刪除動作(因為九宮格要切換字元)
            else sameButtonClickCount = 0;  //代表這次按鍵與上次不同顆或者連擊次數中斷，所以歸零

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
                return en_circularIME_view_left.ACTION_DOWN_EVENT(posTouchDown);

            case 1:     //兩手鍵盤回傳點擊結果
                return en_circularIME_view_twoHands.ACTION_DOWN_EVENT(posTouchDown);

            case 2:     //右手鍵盤回傳點擊結果
                return en_circularIME_view_right.ACTION_DOWN_EVENT(posTouchDown);

            default:
                return "";
        }
    }

    private void IMEkeyboardBackgroundSwitch(String switchEvent)    /**切換鍵盤畫面*/
    {

        switch(switchEvent) {

            case "switchToOtherIME":  /**左手輸入法←→兩手輸入法←→右手輸入法 背景圖更換*/

                IMEclassNumber++;

                switch (IMEclassNumber % 3) {
                    case 0:     //變更為左手鍵盤的背景及設定

                        en_circularIME_view_left.setCircularRadius_enLeft(screenWidth, screenHeight, view_width, view_height);
                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_normal_circular_5x3_ime_background_en_lowercase);
                        flag_uppercase = false;
                        break;

                    case 1:     //變更為雙手鍵盤的背景及設定

                        en_circularIME_view_twoHands.setCircularRadius_enTwoHand(screenWidth, screenHeight, view_width, view_height, false);
                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_lowercase);
                        flag_uppercase = false;
                        break;

                    case 2:     //變更為右手鍵盤的背景及設定

                        en_circularIME_view_right.setCircularRadius_enRight(screenWidth, screenHeight, view_width, view_height);
                        findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_normal_circular_5x3_ime_background_en_lowercase);
                        flag_uppercase = false;
                        break;

                    default:
                        break;
                }
            break;

            case "upperLowerCaseSwitch": /** 英文大寫←→英文小寫 背景圖更換 */

                switch(IMEclassNumber % 3) {
                    case 0:     //左手鍵盤

                        if(flag_uppercase)
                            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_normal_circular_5x3_ime_background_en_uppercase);
                        else
                            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_left_normal_circular_5x3_ime_background_en_lowercase);
                        break;

                    case 1:     //雙手鍵盤

                        if(flag_uppercase)
                            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_uppercase);
                        else
                            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_twohands_normal_5x3_ime_background_lowercase);
                        break;

                    case 2:     //右手鍵盤

                        if(flag_uppercase)
                            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_normal_circular_5x3_ime_background_en_uppercase);
                        else
                            findViewById(R.id.keyboard).setBackgroundResource(R.drawable.en_right_normal_circular_5x3_ime_background_en_lowercase);
                        break;
                }
                break;

            case "numeralSymbolSwitch": /** 數字鍵盤←→標點符號鍵盤 背景圖更換 */

                switch(IMEclassNumber %3){

                    case 0://左手鍵盤
                        switch (en_circularIME_view_left.switchKeyboardArray()){

                            case 0:
                                if(flag_uppercase)
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

                    case 1://右手鍵盤

                        switch (en_circularIME_view_twoHands.switchKeyboardArray()){

                            case 0:
                                if(flag_uppercase)
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

                        switch (en_circularIME_view_right.switchKeyboardArray()){

                            case 0:
                                if(flag_uppercase)
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
                break;
        }
    }

}