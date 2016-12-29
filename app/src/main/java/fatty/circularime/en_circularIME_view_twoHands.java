package fatty.circularime;

import android.graphics.PointF;

/**
 * Created by Fatty on 2016-12-09.
 */

public class en_circularIME_view_twoHands {

    private float screenWidth_view, screenHeight_view, view_width_view, view_height_view;
    private boolean adEvent = false;

    private boolean isHorizontal = true; //true : Horizontal ; false : vertical
    private int swtichKeyboardCounter = 0;
    final private String[][] EN_twoHands_keyboardArray = {
            {"63", "pqrs", "tuv", "wxyz", ""},
            {"44", "ghi", "jkl", "mno", "1#"},
            {"46", "-1", "abc", "def", "33"},
            {"-4", "32", "-5", "", ""}};
    /*          ↑上方矩陣視覺化↓ 上下顛倒左右相反，不能直接按照[row][col]這樣直接一對一對應

                ｜ENTER｜空白鍵｜DEL ｜     ｜     ｜
                ｜  .  ｜大小寫｜ABC ｜DEF  ｜!    ｜
                ｜  ,  ｜GHI  ｜JKL ｜MNO  ｜ 12# ｜
                ｜  ?  ｜PQRS ｜TUV ｜WXYZ ｜     ｜

                註1：12# 意思為 切數字/標點符號鍵盤。
                註2：沒有內容代表目前尚未設定功能。
    */
    final private String[][] EN_twoHands_numeral_keyboardArray = {   //數字鍵盤
            {"63", "55", "56", "57", "48"},
            {"44", "52", "53", "54", "1#"},
            {"46", "49", "50", "51", "33"},
            {"-4", "32", "-5", "", ""}};
    /*      ↑上方矩陣視覺化↓ 上下顛倒左右相反，不能直接按照[row][col]這樣直接一對一對應

                ｜ENTER｜空白鍵｜ DEL｜     ｜     ｜
                ｜  .  ｜  1  ｜  2 ｜  3  ｜  !  ｜
                ｜  ,  ｜  4  ｜  5 ｜  6  ｜ 12# ｜
                ｜  ?  ｜  7  ｜  8 ｜  9  ｜  0  ｜

                註1：12# 意思為 切數字/標點符號鍵盤。
                註2：沒有內容代表目前尚未設定功能。
    */
    final private String[][] EN_twoHands_symbol_keyboardArray = {
            {"63", "40", "41", "35", "59"},
            {"44", "58", "45", "95", "1#"},
            {"46", "64", "39", "34", "33"},
            {"-4", "32", "-5", "", ""}};
    /*          ↑上方矩陣視覺化↓ 上下顛倒左右相反，不能直接按照[row][col]這樣直接一對一對應

                ｜ENTER｜空白鍵｜ DEL｜     ｜     ｜
                ｜  .  ｜  @  ｜  ' ｜  "  ｜  !  ｜
                ｜  ,  ｜  :  ｜  - ｜  _  ｜ 12# ｜
                ｜  ?  ｜  (  ｜  ) ｜  #  ｜  ;  ｜

                註1：12# 意思為 切數字/標點符號鍵盤。
                註2：沒有內容代表目前尚未設定功能。
    */
    private  String[][] keyboardArray = {   //初始值為字母鍵盤
            {"63", "pqrs", "tuv", "wxyz", ""},
            {"44", "ghi", "jkl", "mno", "1#"},
            {"46", "-1", "abc", "def", "33"},
            {"-4", "32", "-5", "", ""}};

    public en_circularIME_view_twoHands() {

        screenWidth_view = 0;
        screenHeight_view = 0;
        view_width_view = 0;
        view_height_view = 0;
    }   /**初始化*/

    public void setCircularRadius_enTwoHand(int screenWidth, int screenHeight, int view_width, int view_height, boolean screeenMode) {

        screenWidth_view = screenWidth;
        screenHeight_view = screenHeight;
        view_width_view = view_width;
        view_height_view = view_height;
        isHorizontal = screeenMode;

        swtichKeyboardCounter = 0;
        System.arraycopy(EN_twoHands_keyboardArray, 0, keyboardArray, 0, EN_twoHands_keyboardArray.length);
    }   /**設定鍵盤座標資訊*/

    public String ACTION_DOWN_EVENT(PointF posTouchDown, boolean enableAD ,boolean adEvent){
        /**return #1:do nothing(在外圈無用地帶) #2:切換左右雙手輸入法 #3:切換英中日輸入法 other:output String  #4:廣告*/

        if (isHorizontal){ //true : Horizontal ; false : vertical

                return getButtoned(posTouchDown, true);
        }else{

            if ((screenHeight_view - posTouchDown.y) < 0.2f*view_height_view) { //點擊座標在最下面那橫條，視為無效操作

                int rowMultiple = (int) ((screenHeight_view - posTouchDown.y) / (0.2f*view_height_view));
                int columnMultiple = (int) (posTouchDown.x / (view_width_view/90));

                if (rowMultiple == 0 && ( 75 <= columnMultiple && columnMultiple < 90))
                    return "#2";

                if (rowMultiple == 0 && ( 0 <= columnMultiple && columnMultiple < 15  && enableAD)){
                    return "#4";
                }else if (rowMultiple == 0 && ( 54 <= columnMultiple && columnMultiple < 65) && adEvent) {
                    return "#5";
                }else if (rowMultiple == 0 && ( 65 <= columnMultiple && columnMultiple < 75) && adEvent) {
                    return "#4";
                }

                return "#1";
            }
            else if(false) { //todo 切換中英日輸入法  待決定區域
                return "#3";
            } else {
                return getButtoned(posTouchDown);
            }
        }

    }   /**回應按下的結果*/

    public int switchKeyboardArray()    /**更換鍵盤種類 字母鍵盤→數字鍵盤→標點符號鍵盤→loop */
    {
        swtichKeyboardCounter++;

        switch (swtichKeyboardCounter % 3){

            case 0: //字母鍵盤
                System.arraycopy(EN_twoHands_keyboardArray, 0, keyboardArray, 0, EN_twoHands_keyboardArray.length);
                return 0;
            case 1: //數字鍵盤
                System.arraycopy(EN_twoHands_numeral_keyboardArray, 0, keyboardArray, 0, EN_twoHands_numeral_keyboardArray.length);
                return 1;
            case 2: //標點符號鍵盤
                System.arraycopy(EN_twoHands_symbol_keyboardArray, 0, keyboardArray, 0, EN_twoHands_symbol_keyboardArray.length);
                return 2;
            default:
                return -1;
        }
    }

    private String getButtoned(PointF posTouchDown)     /**計算該座標所代表的按鈕*/
    {

        int whichColumn = 0;

        //找這座標在第幾圈
        //以1/5的view作為被除數，計算在哪一個row
        //等於0 第一圈(最內圈) 中英日切換輸入法，但由於在input_onTouchUp內已經篩選過，所以這裡不會出現
        //等於1 第二圈
        //等於2 第三圈
        //等於3 第四圈
        //等於4 第五圈 (最外圈)
        int rowMultiple = (int) ((screenHeight_view - posTouchDown.y) / (0.2f*view_height_view));

        int columnMultiple = (int) (posTouchDown.x / (view_width_view/90));

        if (rowMultiple == 4) {        //如果在最外圈

            if (columnMultiple < 30)
                whichColumn = 0;       //最外圈ENTER鍵
            else if (columnMultiple < 60)
                whichColumn = 1;       //最外圈空白鍵
            else
                whichColumn = 2;       //最外圈DEL鍵

        } else {                       //直的

            if (columnMultiple < 15)
                whichColumn = 0;       //小於15，whichColumn。(?；,；.)
            else if (columnMultiple < 35)
                whichColumn = 1;       //小於35，whichColumn。(PQRS；GHI；↑)
            else if (columnMultiple <= 55)
                whichColumn = 2;       //小於55，whichColumn。(TUV；JKL；ABC)
            else if (columnMultiple <= 75)
                whichColumn = 3;       //小於75，whichColumn。(WXYZ；MNO；DEF)
            else
                whichColumn = 4;       //小於90，whichColumn。(NULL；12#；)
        }

    return keyboardArray[rowMultiple-1][whichColumn];
    }

    private String getButtoned(PointF posTouchDown , boolean horizontal)     /**計算該座標所代表的按鈕*/
    {

        int whichColumn = 0;

        //找這座標在第幾圈
        //以1/4的view作為被除數，計算在哪一個row
        //等於0 第一圈
        //等於1 第二圈
        //等於2 第三圈
        //等於3 第四圈 (最外圈)
        int rowMultiple = (int) ((screenHeight_view - posTouchDown.y) / (0.25f*view_height_view));

        int columnMultiple = (int) (posTouchDown.x / (view_width_view/90));

        if (rowMultiple == 3) {        //如果在最外圈

            if (columnMultiple < 30)
                whichColumn = 0;       //最外圈ENTER鍵
            else if (columnMultiple < 60)
                whichColumn = 1;       //最外圈空白鍵
            else
                whichColumn = 2;       //最外圈DEL鍵

        } else {                       //直的

            if (columnMultiple < 15)
                whichColumn = 0;       //小於15，whichColumn。(?；,；.)
            else if (columnMultiple < 35)
                whichColumn = 1;       //小於35，whichColumn。(PQRS；GHI；↑)
            else if (columnMultiple <= 55)
                whichColumn = 2;       //小於55，whichColumn。(TUV；JKL；ABC)
            else if (columnMultiple <= 75)
                whichColumn = 3;       //小於75，whichColumn。(WXYZ；MNO；DEF)
            else
                whichColumn = 4;       //小於90，whichColumn。(NULL；12#；)
        }

        return keyboardArray[rowMultiple][whichColumn];
    }
}
