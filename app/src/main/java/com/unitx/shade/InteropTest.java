package com.unitx.shade;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.unitx.shade_core.common.DocumentMimeType;
import com.unitx.shade_core.core.ShadeCore;
import com.unitx.shade_core.launcher.Shade;
import com.unitx.shade_core.common.action.ShadeAction;
import com.unitx.shade_core.common.result.ShadeResult;

public class InteropTest extends AppCompatActivity {

    private static final String TAG = "InteropTest";

    // Class-level property — required for Activity binding (not `by lazy` equivalent / not inside onCreate lambda)
    private ShadeCore shade;

    private ImageView imagePreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imagePreview = findViewById(R.id.imagePreview);

        // No Shade.INSTANCE.with(...), no return null; anywhere below.
        shade = Shade.with(InteropTest.this, config -> {

            config.image(imageConfig -> {

                imageConfig.camera(cameraConfig -> {
                    cameraConfig.compress(compressConfig -> {
                        compressConfig.setEnabled(true);
                        compressConfig.setQuality(80);
                        compressConfig.setMaxWidth(1024);
                        compressConfig.setMaxHeight(1024);
                    });

                    cameraConfig.onResult(result -> {
                        Log.d(TAG, "Image camera captured: " + result.getUri());
                        imagePreview.setImageURI(result.getUri());
                    });

                    cameraConfig.onFailure(error -> {
                        Log.e(TAG, "Image camera failed: " + error);
                        Toast.makeText(this, "Camera failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                });

                imageConfig.gallery(galleryConfig -> {
                    galleryConfig.multiSelect(multiSelectConfig -> {
                        multiSelectConfig.setEnabled(true);
                        multiSelectConfig.setMaxItems(5);
                    });

                    galleryConfig.onResult(result -> {
                        if (result instanceof ShadeResult.Single) {
                            ShadeResult.Single single = (ShadeResult.Single) result;
                            Log.d(TAG, "Image gallery single: " + single.getUri());
                            imagePreview.setImageURI(single.getUri());
                        } else if (result instanceof ShadeResult.Multiple) {
                            ShadeResult.Multiple multiple = (ShadeResult.Multiple) result;
                            Log.d(TAG, "Image gallery multiple: " + multiple.getItems().size() + " items");
                        }
                    });

                    galleryConfig.onFailure(error -> {
                        Log.e(TAG, "Image gallery failed: " + error);
                    });
                });
            });

            config.video(videoConfig -> {

                videoConfig.camera(videoCameraConfig -> {
                    videoCameraConfig.setDurationLimit(30);

                    videoCameraConfig.compress(compressConfig -> {
                        compressConfig.setEnabled(true);
                        compressConfig.setVideoBitrate(1_500_000);
                    });

                    videoCameraConfig.onResult(result -> {
                        Log.d(TAG, "Video camera captured: " + result.getUri());
                    });

                    videoCameraConfig.onFailure(error -> {
                        Log.e(TAG, "Video camera failed: " + error);
                    });
                });

                videoConfig.gallery(galleryConfig -> {
                    galleryConfig.onResult(result -> {
                        Log.d(TAG, "Video gallery result: " + result);
                    });

                    galleryConfig.onFailure(error -> {
                        Log.e(TAG, "Video gallery failed: " + error);
                    });
                });
            });

            config.document(documentConfig -> {
                documentConfig.copyToCache(cacheConfig -> {
                    cacheConfig.setEnabled(true);
                    cacheConfig.onProgress(progress -> Log.d(TAG, "Document copy progress: " + progress));
                });

                documentConfig.onResult(result -> {
                    Log.d(TAG, "Document picked: " + result);
                });

                documentConfig.onFailure(error -> {
                    Log.e(TAG, "Document pick failed: " + error);
                });
            });
        });

        findViewById(R.id.btnImageCamera).setOnClickListener(v -> shade.launch(ShadeAction.Image.Camera.INSTANCE));

        findViewById(R.id.btnImageGallery).setOnClickListener(v -> shade.launch(ShadeAction.Image.Gallery.INSTANCE));

        findViewById(R.id.btnVideoCamera).setOnClickListener(v -> shade.launch(ShadeAction.Video.Camera.INSTANCE));

        findViewById(R.id.btnVideoGallery).setOnClickListener(v -> shade.launch(ShadeAction.Video.Gallery.INSTANCE));

        findViewById(R.id.btnDocument).setOnClickListener(v -> shade.launch(new ShadeAction.Document(DocumentMimeType.ALL_ENTRY_LIST)));
    }
}