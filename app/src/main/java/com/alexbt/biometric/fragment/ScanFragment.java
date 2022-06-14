package com.alexbt.biometric.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Pair;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.alexbt.biometric.MyApplication;
import com.alexbt.biometric.R;
import com.alexbt.biometric.fragment.viewmodel.JotformMemberViewModel;
import com.alexbt.biometric.model.Member;
import com.alexbt.biometric.util.DateUtils;
import com.alexbt.biometric.util.ImageUtils;
import com.alexbt.biometric.util.RequestUtil;
import com.alexbt.biometric.util.UrlUtils;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScanFragment extends Fragment implements View.OnClickListener {
    private int cam_face = CameraSelector.LENS_FACING_FRONT;
    private boolean flipX = false;
    private ProcessCameraProvider cameraProvider;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private FaceDetector detector;
    private Interpreter tfLite;
    private float distance = 0.75f;
    private CountDownTimer countDownTimer;
    private boolean isMemberIdentified = false;
    private Spinner scannedMember;
    private ArrayAdapter<Member> adapter;
    private TextView checkinButton;
    private TextView resetButton;
    private TextView checkinStatus;
    private TextView countdown;
    private JotformMemberViewModel jotformMemberViewModel;
    private Map<String, String> processed = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null || getActivity() == null || getContext() == null) {
            return null;
        }
        View root = inflater.inflate(R.layout.fragment_scan, container, false);

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);

        jotformMemberViewModel = JotformMemberViewModel.getModel(this,
                UrlUtils.getMembersUrl(getContext()),
                UrlUtils.updateMembersUrl(getContext()),
                UrlUtils.addMembersUrl(getContext()));

        jotformMemberViewModel.getJotformMembersOrFetch().observe(this.getViewLifecycleOwner(), new Observer<Set<Member>>() {
            @Override
            public void onChanged(Set<Member> members) {
                if (members == null || getContext() == null) {
                    return;
                }
                adapter.addAll(members);
                adapter.sort(Member.getSortComparator());
                adapter.insert(new Member(null, null, getString(R.string.MEMBRE_EXISTANT_INCOMPLET), "", "", "", null), 0);
                adapter.notifyDataSetChanged();
            }
        });

        View camera_switch = root.findViewById(R.id.camera_switch_button);
        previewView = root.findViewById(R.id.camera_preview_image);
        countdown = root.findViewById(R.id.countdown);
        checkinButton = root.findViewById(R.id.checkin_button);
        checkinStatus = root.findViewById(R.id.checkinStatus);
        resetButton = root.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetMemberIdentification();
            }
        });

        countDownTimer = new CountDownTimer(3000, 1000) {
            public void onTick(long millisUntilFinished) {
                long remaining = millisUntilFinished / 1000 + 1;
                countdown.setText(String.format(Locale.getDefault(), "%d", remaining));
            }

            public void onFinish() {
                doCheckin(true);
                resetButton.callOnClick();
            }
        };
        checkinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCheckin(false);
            }
        });
        scannedMember = root.findViewById(R.id.scannedMember);

        adapter = new ArrayAdapter<Member>(getActivity().getApplicationContext(), R.layout.abt_two_line_list_item) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return adaptView(position, convertView);
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return adaptView(position, convertView);
            }

            private View adaptView(final int position, View convertView) {
                if (getActivity() == null || getContext() == null) {
                    return convertView;
                }
                LinearLayout twoLineListItem;

                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    twoLineListItem = (LinearLayout) inflater.inflate(R.layout.abt_two_line_list_item, null);
                } else {
                    twoLineListItem = (LinearLayout) convertView;
                }
                Member member = this.getItem(position);
                TextView item1 = (TextView) twoLineListItem.getChildAt(0);
                TextView item2 = (TextView) twoLineListItem.getChildAt(1);
                item1.setText(String.format("%s %s", member.getFirstName(), member.getLastName()));
                item2.setText(String.format("%s", member.getEmail()));

                if (position % 2 == 0) {
                    twoLineListItem.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    twoLineListItem.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.chrome));
                }
                if (position == 0) {
                    item1.setGravity(Gravity.BOTTOM);
                    item1.setPadding(item1.getPaddingLeft(), item1.getTotalPaddingTop(), item1.getPaddingRight(), 0);
                    item1.setHeight(item1.getLineHeight() + item2.getLineHeight() / 2);
                    item2.setHeight(item2.getLineHeight() / 2);
                }

                twoLineListItem.setOnTouchListener(new AdapterView.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (scannedMember.getSelectedItemPosition() != position) {
                            if (position == 0) {
                                isMemberIdentified = false;
                            } else {
                                countDownTimer.cancel();
                                enableCheckinButton(true);
                                countdown.setText("");
                                Member member = (Member) scannedMember.getSelectedItem();
                                checkinStatus.setText(getLastCheckinText(member));
                                isMemberIdentified = true;
                            }
                            resetButton.setEnabled(isMemberIdentified);
                        }
                        return false;
                    }
                });

                return twoLineListItem;
            }

        };

        scannedMember.setAdapter(adapter);
        scannedMember.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                enableCheckinButton(i > 0);
                Member member = (Member) scannedMember.getItemAtPosition(i);
                checkinStatus.setText(getLastCheckinText(member));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        camera_switch.setOnClickListener(v -> {
            if (getActivity() == null || getContext() == null) {
                return;
            }
            if (cam_face == CameraSelector.LENS_FACING_BACK) {
                cam_face = CameraSelector.LENS_FACING_FRONT;
                flipX = true;
            } else {
                cam_face = CameraSelector.LENS_FACING_BACK;
                flipX = false;
            }
            cameraProvider.unbindAll();
            cameraBind();
        });

        try {
            tfLite = new Interpreter(ImageUtils.loadModelFile(getActivity(), "mobile_face_net.tflite").asReadOnlyBuffer());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Initialize Face Detector
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();
        detector = FaceDetection.getClient(highAccuracyOpts);
        cameraBind();

        distance = sharedPreferences.getFloat("distanceProp", 0.75f);
        if (distance < 0.45f || distance >= 1.0f) {
            distance = 0.75f;
            getActivity().getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE)
                    .edit()
                    .putFloat("distanceProp", 0.75f)
                    .apply();
        }
        return root;
    }

    private void enableCheckinButton(boolean toEnable) {
        checkinButton.setEnabled(toEnable);
        if (checkinButton.isEnabled()) {
            checkinButton.setTextColor(getResources().getColor(R.color.azure));
        } else {
            checkinButton.setTextColor(resetButton.getCurrentTextColor());
        }
    }

    private void resetMemberIdentification() {
        if (scannedMember != null) {
            scannedMember.setSelection(0);
        }
        if (checkinStatus != null) {
            checkinStatus.setText("");
        }
        if (resetButton != null) {
            resetButton.setEnabled(false);
        }
        if (countdown != null) {
            countdown.setText("");
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isMemberIdentified = false;
            }
        }, 1000);
    }

    @NonNull
    private String getLastCheckinText(Member member) {
        String current = DateUtils.getCurrentDate();
        String text;
        if (member.getLastCheckin() == null) {
            text = "";
        } else if (current.equals(member.getLastCheckin())) {
            text = String.format("Dernier checkin: %s (aujourd'hui)", member.getLastCheckin());
        } else {
            text = String.format("Dernier checkin: %s", member.getLastCheckin());
        }
        return text;
    }

    private void cameraBind() {
        if (getActivity() == null || getContext() == null) {
            return;
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this in Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cam_face)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
                        .build();

        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, imageProxy -> {
            try {
                Thread.sleep(0);  //Camera preview refreshed every 10 millisec(adjust as required)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            InputImage image;


            @SuppressLint("UnsafeExperimentalUsageError")
            // Camera Feed-->Analyzer-->ImageProxy-->mediaImage-->InputImage(needed for ML kit face detection)

            Image mediaImage = imageProxy.getImage();

            if (mediaImage != null) {
                image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                detector.process(image).addOnSuccessListener(
                        faces -> {
                            if (isMemberIdentified) {
                                return;
                            }
                            if (faces.size() != 0) {

                                Face face = faces.get(0); //Get first face from detected faces
//                                                    System.out.println(face);

                                //mediaImage to Bitmap
                                Bitmap frame_bmp = ImageUtils.toBitmap(mediaImage);

                                int rot = imageProxy.getImageInfo().getRotationDegrees();

                                //Adjust orientation of Face
                                Bitmap frame_bmp1 = ImageUtils.rotateBitmap(frame_bmp, rot, false, false);

                                //Get bounding box of face
                                RectF boundingBox = new RectF(face.getBoundingBox());

                                //Crop out bounding box from whole Bitmap(image)
                                Bitmap cropped_face = ImageUtils.getCropBitmapByCPU(frame_bmp1, boundingBox);

                                if (flipX) {
                                    cropped_face = ImageUtils.rotateBitmap(cropped_face, 0, flipX, false);
                                }
                                //Scale the acquired Face to 112*112 which is required input for model
                                Bitmap scaled = ImageUtils.getResizedBitmap(cropped_face, 112, 112);
                                recognizeImage(scaled); //Send scaled bitmap to create face embeddings.
                            }
                        })
                        .addOnFailureListener(
                                e -> {
                                })
                        .addOnCompleteListener(task -> {
                            imageProxy.close(); //v.important to acquire next frame for analysis
                        });
            }
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }

    @Override
    public void onResume() {
        super.onResume();
        jotformMemberViewModel.getJotformMembersOrFetch(this.getActivity());
    }

    private void recognizeImage(final Bitmap bitmap) {
        // set Face to Preview
        //Create ByteBuffer to store normalized image

        //Input size for model
        int inputSize = 112;
        ByteBuffer imgData = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);

        imgData.order(ByteOrder.nativeOrder());

        int[] intValues = new int[inputSize * inputSize];

        //get pixel values from Bitmap to normalize
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();

        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                boolean isModelQuantized = false;
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    float IMAGE_MEAN = 128.0f;
                    float IMAGE_STD = 128.0f;
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                }
            }
        }
        //imgData is input to our model
        Object[] inputArray = {imgData};

        Map<Integer, Object> outputMap = new HashMap<>();
        //Output size of model
        int OUTPUT_SIZE = 192;
        float[][] embeedings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
        outputMap.put(0, embeedings);
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model

        float distance_local;

        if (jotformMemberViewModel.getJotformMembersOrFetch().getValue() == null || jotformMemberViewModel.getJotformMembersOrFetch().getValue().isEmpty()) {
            return;
        }
        final List<Pair<Member, Float>> nearest = ImageUtils.findNearest(embeedings[0], jotformMemberViewModel.getJotformMembersOrFetch().getValue());//Find 2 closest matching face
        if (nearest.isEmpty()) {
            return;
        }

        if (nearest.get(0) != null && getContext() != null) {
            Member matchingMember = nearest.get(0).first;
            distance_local = nearest.get(0).second;
            if (distance_local < distance) {
                int spinnerPosition = adapter.getPosition(matchingMember);
                isMemberIdentified = true;
                scannedMember.setSelection(spinnerPosition);
                resetButton.setEnabled(true);
                countDownTimer.start();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            resetMemberIdentification();
        } catch (RuntimeException e) {
            //ignore
        }
    }


    @Override
    public void onClick(View view) {
        if (getActivity() == null || getContext() == null) {
            return;
        }
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.navigation_scan);
    }

    public void doCheckin(final boolean sourceAutomatic) {
        if (getContext() == null || getActivity() == null) {
            return;
        }
        Member member = (Member) scannedMember.getSelectedItem();
        try {
            Date dateTime = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formattedDate = df.format(dateTime);

            String memberLastCheckin = processed.get(member.getMemberId());
            if (sourceAutomatic && memberLastCheckin != null && memberLastCheckin.equals(formattedDate)) {
                Toast.makeText(getActivity(), String.format("Présence DÉJÀ enregistrée pour %s %s", member.getFirstName(), member.getLastName()), Toast.LENGTH_SHORT).show();
                return;
            }

            RequestUtil.sendCheckin(member, dateTime, formattedDate, getActivity(), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    processed.put(member.getMemberId(), formattedDate);
                    new Handler().postDelayed(() -> processed.remove(member.getMemberId()), 75 * 1000 * 60);
                    checkinStatus.setText(getLastCheckinText(member));
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
            if (!sourceAutomatic) {
                resetMemberIdentification();
            }
        } catch (Exception e) {
            MyApplication.saveError(getContext(), e);
        }
    }

}
