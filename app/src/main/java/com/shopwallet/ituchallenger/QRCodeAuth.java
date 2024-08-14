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
/**
 * QRCodeAuth is an activity that handles QR code authentication using the device's camera.
 * It manages camera operations, processes QR code images, and triggers local authentication mechanisms.
 */
public class QRCodeAuth extends AppCompatActivity {

    // Constants
    private static final String TAG = "QRCodeAuthClass";  // Tag for logging
    private static final int CAMERA_PERMISSION_CODE = 100;  // Request code for camera permission

    // UI Components
    private TextureView textureView;  // View to display camera preview
    private CameraDevice cameraDevice;  // Reference to the device's camera
    private CameraCaptureSession captureSession;  // Session for capturing images
    private CaptureRequest.Builder captureRequestBuilder;  // Builder for camera capture requests
    private Size imageDimension;  // Dimensions for the camera image
    private Handler backgroundHandler;  // Handler for background operations
    private HandlerThread backgroundThread;  // Thread for background operations
    private ImageReader imageReader;  // Reader for images from the camera

    // Flags and variables
    private boolean isQRCodeScanned = false;  // Flag to prevent multiple QR scans
    private String userKey;  // User's key, retrieved from input data
    private String authType;  // Type of authentication to be used
    private String qrId;  // ID extracted from the QR code

    /**
     * Called when the activity is first created. Sets up the UI and initializes camera.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_auth);

        // Initialize the TextureView for camera preview
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(textureListener);

        // Set up the close button to finish the activity
        Button buttonClose = findViewById(R.id.button_close);
        buttonClose.setOnClickListener(view -> finish());

        // Request camera permission if not granted, otherwise open the camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }

        // Retrieve and parse user input data from secure storage
        HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");
        // Assuming inputData is a JSON string. Parse it to get userKey and authType.
        // Adjust the parsing according to your actual data format.
        userKey = (String) inputData.get("userKey");
        authType = (String) inputData.get("authType");
    }

    /**
     * SurfaceTextureListener to handle TextureView events for camera preview.
     */
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

    /**
     * Opens the device's camera and sets up image capture.
     */
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

    /**
     * Creates the camera preview session and starts displaying the camera feed.
     */
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

    /**
     * Starts the camera preview by setting the repeating request.
     */
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

    /**
     * Processes the captured image and extracts QR code data.
     *
     * @param image The captured image from the camera.
     * @throws ChecksumException If there is a checksum error during QR code processing.
     * @throws NotFoundException If the QR code is not found in the image.
     * @throws FormatException If the QR code format is incorrect.
     */
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

    /**
     * Triggers the appropriate local authentication method based on the provided authentication type.
     *
     * @param authType The type of authentication to perform (e.g., biometric or PIN/Pattern).
     * @param userKey  The user's unique key used for authentication.
     * @param qrId     The ID extracted from the scanned QR code.
     */
    private void triggerLocalAuth(String authType, String userKey, String qrId) {
        if (authType.equals("3")) {
            // Trigger biometric authentication
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(QRCodeAuth.this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    // Handle failed biometric authentication
                    qrAuthenticator(userKey, qrId, false);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    // Handle successful biometric authentication
                    qrAuthenticator(userKey, qrId, true);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    // Handle failed biometric authentication attempt
                    qrAuthenticator(userKey, qrId, false);
                }
            });

            // Configure the prompt information for biometric authentication
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Scan your fingerprint to authenticate")
                    .setNegativeButtonText("Cancel")
                    .build();

            // Start biometric authentication
            runOnUiThread(() -> biometricPrompt.authenticate(promptInfo));

        } else if (authType.equals("4")) {
            // Trigger PIN/Pattern authentication
            triggerPinPatternAuth(userKey, qrId);
        }
    }

    /**
     * Triggers the PIN or Pattern authentication using the device's secure lock screen.
     *
     * @param userKey The user's unique key used for authentication.
     * @param qrId    The ID extracted from the scanned QR code.
     */
    private void triggerPinPatternAuth(String userKey, String qrId) {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardSecure()) {
            // Launch the secure lock screen for PIN/Pattern authentication
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication Required", "Please enter your PIN/Pattern to authenticate");
            if (intent != null) {
                deviceCredentialLauncher3.launch(intent);
            } else {
                // Authentication not available
                qrAuthenticator(userKey, qrId, false);
            }
        } else {
            // Device is not secure (no PIN/Pattern set)
            qrAuthenticator(userKey, qrId, false);
        }
    }

    /**
     * A launcher to handle the result of the secure lock screen authentication.
     */
    private final ActivityResultLauncher<Intent> deviceCredentialLauncher3 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Authentication successful
                    qrAuthenticator(userKey, qrId, true);
                } else {
                    // Authentication failed or canceled
                    Log.e(TAG, "Device is not secure");
                    Snackbar.make(findViewById(android.R.id.content), "Device is not secure", Snackbar.LENGTH_SHORT).show();
                }
            }
    );

    /**
     * Handles the result of the QR code authentication process.
     *
     * @param userKey The user's unique key used for authentication.
     * @param qrId    The ID extracted from the scanned QR code.
     * @param isAuth  The authentication result (true if successful, false otherwise).
     */
    private void qrAuthenticator(String userKey, String qrId, boolean isAuth) {
        Log.e(TAG, "userKey: " + userKey + " | qrId: " + qrId + " | isAuth: " + isAuth);

        // Show a progress dialog while processing the authentication request
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Perform the QR code authentication using the BsaSdk
        BsaSdk.getInstance().getSdkService().qrAuthenticator(userKey, qrId, isAuth,
                this, new SdkAuthResponseCallback<>() {
                    @Override
                    public void onSuccess(AuthCompleteResponse result) {
                        // Handle successful QR code authentication
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
                        // Handle the processing state of the QR code authentication
                        Log.e(TAG, "QR code authentication processing...: s: " + s);
                        Snackbar.make(findViewById(android.R.id.content), "QR code authentication processing...: s: " + s, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(ErrorResult errorResult) {
                        // Handle failed QR code authentication
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

    /**
     * Starts the background thread for camera operations.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Stops the background thread for camera operations.
     */
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

    /**
     * Starts the background thread for handling camera operations.
     */
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

    /**
     * Stops the background thread and cleans up resources.
     */
    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * Closes the camera resources, including the capture session, camera device, and image reader.
     * Ensures that all camera-related resources are properly released to avoid memory leaks or other issues.
     */
    private void closeCamera() {
        if (captureSession != null) {
            try {
                // Stop any ongoing capture session and close it
                captureSession.stopRepeating();
                captureSession.close();
                captureSession = null; // Set to null to mark as released
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        if (cameraDevice != null) {
            // Close the camera device
            cameraDevice.close();
            cameraDevice = null; // Set to null to mark as released
        }

        if (imageReader != null) {
            // Close the image reader
            imageReader.close();
            imageReader = null; // Set to null to mark as released
        }
    }

    /**
     * Handles the result of the permission request for camera access.
     * If the camera permission is granted, the camera is opened; otherwise, a message is shown indicating
     * that camera permission is required to use the QR code scanner.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The results of the permission requests.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open the camera
                openCamera();
            } else {
                // Permission denied, show a message indicating that camera permission is required
                Toast.makeText(this, "Camera permission is required to use QR code scanner", Toast.LENGTH_SHORT).show();
            }
        }
    }
}