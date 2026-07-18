package com.unitx.shade;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.unitx.shade_core.launcher.Shade;


public class InteropTest extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Shade.with(InteropTest.this, config -> {
            config.image(imageConfig-> {
                imageConfig.camera(cameraConfig-> {
                    cameraConfig.compress(compressionConfig -> {

                    });
                });
            });
        });
    }
}
