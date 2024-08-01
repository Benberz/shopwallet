package com.shopwallet.ituchallenger;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkAuthResponseCallback;
import com.fnsv.bsa.sdk.response.AuthCompleteResponse;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodeAuth extends AppCompatActivity {

    private static final String TAG = "QRCodeAuthClass";

    private static final int CAMERA_PERMISSION_CODE = 100;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private ImageReader imageReader;

    private boolean isQRCodeScanned = false;

    private String userKey;
    private String authType;
    private String qrId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_auth);

        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(textureListener);

        Button buttonClose = findViewById(R.id.button_close);
        buttonClose.setOnClickListener(view -> finish());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }

        HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");
        // Assuming inputData is a JSON string. Parse it to get userKey and authType.
        // Adjust the parsing according to your actual data format.
        userKey = (String) inputData.get("userKey");
        authType = (String) inputData.get("authType");
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
                Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
                int width = 640;
                int height = 480;
                if (jpegSizes != null && jpegSizes.length > 0) {
                    width = jpegSizes[0].getWidth();
                    height = jpegSizes[0].getHeight();
                }
                imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
                imageReader.setOnImageAvailableListener(reader -> {
                    if (!isQRCodeScanned) {
                        try (Image image = reader.acquireLatestImage()) {
                            if (image != null) {
                                processImage(image);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, backgroundHandler);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                return;
            }
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            Surface imageSurface = imageReader.getSurface();
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageSurface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }
                    captureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(QRCodeAuth.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void processImage(Image image) throws ChecksumException, NotFoundException, FormatException {
        try (image) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            // Y plane
            yBuffer.get(nv21, 0, ySize);

            // U and V planes
            byte[] uBytes = new byte[uSize];
            byte[] vBytes = new byte[vSize];

            uBuffer.get(uBytes);
            vBuffer.get(vBytes);

            for (int i = 0; i < uSize; i++) {
                nv21[ySize + i * 2] = vBytes[i];
                nv21[ySize + i * 2 + 1] = uBytes[i];
            }

            LuminanceSource source = new PlanarYUVLuminanceSource(nv21, image.getWidth(), image.getHeight(), 0, 0,
                    image.getWidth(), image.getHeight(), false);

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Reader reader = new MultiFormatReader();
            Result result = reader.decode(bitmap);
            isQRCodeScanned = true; // Set flag to prevent further scans

            // Extract the value between the colon and the question mark
            String qrText = result.getText();
            Pattern pattern = Pattern.compile("platform=CMMAPF000:([^?]+)\\?secret");
            Matcher matcher = pattern.matcher(qrText);
            if (matcher.find()) {
                qrId = matcher.group(1);
            } else {
                // Handle the case where the expected pattern is not found
                qrId = ""; // Or any default/fallback value
            }

            Log.e(TAG, "--------> qrText: " + qrText);
            Log.e(TAG, "--------> qrId: " + qrId);
            triggerLocalAuth(authType, userKey, qrId);
            // runOnUiThread(() -> showQRCodeDialog(result.getText()));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    private void triggerLocalAuth(String authType, String userKey, String qrId) {
        if (authType.equals("3")) {
            // Trigger biometric authentication
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(QRCodeAuth.this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    qrAuthenticator(userKey, qrId, false); // Failed authentication
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    qrAuthenticator(userKey, qrId, true); // Successful authentication
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    qrAuthenticator(userKey, qrId, false); // Failed authentication
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Scan your fingerprint to authenticate")
                    .setNegativeButtonText("Cancel")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else if (authType.equals("4")) {
            // Trigger PIN/Pattern authentication (dummy implementation, replace with actual logic)
            // Trigger PIN/Pattern authentication
            triggerPinPatternAuth(userKey, qrId);
        }
    }

    private void triggerPinPatternAuth(String userKey, String qrId) {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication Required", "Please enter your PIN/Pattern to authenticate");
            if (intent != null) {
                deviceCredentialLauncher3.launch(intent);
            } else {
                qrAuthenticator(userKey, qrId, false); // Authentication not available
            }
        } else {
            qrAuthenticator(userKey, qrId, false); // Device is not secure
        }
    }

    private final ActivityResultLauncher<Intent> deviceCredentialLauncher3 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
            if (result.getResultCode() == RESULT_OK) {
                qrAuthenticator(userKey, qrId, true); // Authentication successful
            } else {
                //qrAuthenticator(userKey, qrId, false); // Authentication failed
                Log.e(TAG, "Device is not secure");
                Snackbar.make(findViewById(android.R.id.content), "Device is not secure", Snackbar.LENGTH_SHORT).show();
            }
        }
    );

    private void qrAuthenticator(String userKey, String qrId, boolean isAuth) {

        Log.e(TAG, "userKey: " + userKey + " | qrId: " + qrId + " | isAuth: " + isAuth);
        // Show a dialog while processing
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                .setCancelable(false)
                .create();
        progressDialog.show();

        BsaSdk.getInstance().getSdkService().qrAuthenticator(userKey, qrId, isAuth,
                this, new SdkAuthResponseCallback<>() {
                    @Override
                    public void onSuccess(AuthCompleteResponse result) {
                        Log.d(TAG, "QR code authentication Successful. result code: " + result.rtCode + " | msg: " + result.rtMsg);
                        progressDialog.dismiss();
                        runOnUiThread(() -> new AlertDialog.Builder(QRCodeAuth.this)
                                .setTitle("Authentication Successful")
                                .setMessage("QR code authentication Successful.")
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    dialog.dismiss();
                                    startActivity(new Intent(QRCodeAuth.this, Dashboard.class));
                                    finish();
                                })
                                .show());
                    }

                    @Override
                    public void onProcess(boolean b, String s) {
                        Log.e(TAG, "QR code authentication processing...: s: " + s);
                        Snackbar.make(findViewById(android.R.id.content), "QR code authentication processing...: s: " + s, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(ErrorResult errorResult) {
                        Log.e(TAG, "QR code authentication Failed!! Error: " + errorResult.getErrorCode() + " | Message: " + errorResult.getErrorMessage());
                        progressDialog.dismiss();
                        runOnUiThread(() -> new AlertDialog.Builder(QRCodeAuth.this)
                                .setTitle("Authentication Failed")
                                .setMessage("Error Code: " + errorResult.getErrorCode() + "\nMessage: " + errorResult.getErrorMessage())
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    dialog.dismiss();
                                    startActivity(new Intent(QRCodeAuth.this, Dashboard.class));
                                    finish();
                                })
                                .show());
                    }
                });
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void closeCamera() {
        if (captureSession != null) {
            try {
                captureSession.stopRepeating();
                captureSession.close();
                captureSession = null;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use QR code scanner", Toast.LENGTH_SHORT).show();
            }
        }
    }
}