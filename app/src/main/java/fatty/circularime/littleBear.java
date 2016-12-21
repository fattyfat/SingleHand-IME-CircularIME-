package fatty.circularime;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;

import static fatty.circularime.R.layout.bearhome;

/**
 * Created by Fatty on 2016-12-13.
 */

public class littleBear extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(bearhome);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int)(width*.6),(int)(height*.3));

        Button OK_Button = (Button) findViewById(R.id.OK_Button);
        OK_Button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(littleBear.this,AD_view.class));
            }
        });
    }

}