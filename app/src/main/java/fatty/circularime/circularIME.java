package fatty.circularime;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;

/**
 * Created by Fatty on 2016-11-29.
 */

public class circularIME extends InputMethodService {

    fatty.circularime.circularIME_view circularIME_View;

    @Override
    public View onCreateInputView()
    {
        circularIME_View = (fatty.circularime.circularIME_view) getLayoutInflater().inflate(R.layout.input, null);
        circularIME_View.setIME(this);

        return circularIME_View;
    }

    /** Helper to commit text to input */
    public void outputText(String str)
    {
        getCurrentInputConnection().commitText(str,1);
    }

    /** Helper to send a special key to input */
    public void outputKeyEvent(int keyEventCode)
    {
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    public void delOneText()
    {
        getCurrentInputConnection().deleteSurroundingText(1,1);
    }
}
