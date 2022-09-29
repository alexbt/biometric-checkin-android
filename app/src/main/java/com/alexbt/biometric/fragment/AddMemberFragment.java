package com.alexbt.biometric.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.alexbt.biometric.util.FormatUtil;
import com.alexbt.biometric.util.ImageUtils;
import com.alexbt.biometric.util.InputValidator;
import com.alexbt.biometric.util.UrlUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.apache.commons.text.WordUtils;
import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AddMemberFragment extends Fragment implements View.OnClickListener {
    private int cam_face = CameraSelector.LENS_FACING_FRONT;
    private boolean flipX = false;
    private Interpreter tfLite;
    private AlertDialog dialog;
    private ProcessCameraProvider cameraProvider;
    private ImageView face_preview;
    private FaceDetector detector;
    private float[][] embeedings;
    private static final int SELECT_PICTURE = 1;
    private Spinner spinner;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView inputFirstName;
    private TextView inputPhone;
    private TextView inputLastName;
    private PreviewView previewView;
    private TextView inputEmail;
    private TextView prefilled;

    // Store instance variables based on arguments passed
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

        View root = inflater.inflate(R.layout.fragment_member_add, container, false);

        face_preview = root.findViewById(R.id.captured_image_view);
        View camera_switch = root.findViewById(R.id.camera_switch_button);
        View add_face = root.findViewById(R.id.add_member_button);
        add_face.setOnClickListener((v -> addFace()));

        camera_switch.setOnClickListener(v -> {
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

        if (getActivity() == null || getContext() == null) {
            return null;
        }
        try {
            tfLite = new Interpreter(ImageUtils.loadModelFile(getActivity(), "mobile_face_net.tflite").asReadOnlyBuffer());
        } catch (IOException e) {
            e.printStackTrace();
        }
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();
        detector = FaceDetection.getClient(highAccuracyOpts);
        cameraBind();
        return root;
    }

    @Override
    public void onClick(View view) {
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
        if (getActivity() == null || getContext() == null) {
            return;
        }

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cam_face)
                .build();

        previewView = getActivity().findViewById(R.id.camera_preview_image);
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
                            if (dialog != null && dialog.isShowing()) {
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

    public void recognizeImage(final Bitmap bitmap) {

        // set Face to Preview
        face_preview.setImageBitmap(bitmap);

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
        embeedings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
        outputMap.put(0, embeedings);
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model
    }


    private void addFace() {
        if (getActivity() == null || getContext() == null) {
            return;
        }

        cameraProvider.unbindAll();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.ADD_MEMBER);
        builder.setView(R.layout.fragment_member_add_form);

        ArrayAdapter<Member> adapter = new ArrayAdapter<Member>(getActivity().getApplicationContext(), R.layout.abt_two_line_list_item) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return adaptView(position, convertView);
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return adaptView(position, convertView);
            }

            private View adaptView(int position, View convertView) {
                LinearLayout twoLineListItem;

                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    twoLineListItem = (LinearLayout) inflater.inflate(R.layout.abt_two_line_list_item, null);
                } else {
                    twoLineListItem = (LinearLayout) convertView;
                }
                Member member = this.getItem(position);
                TextView textView1 = (TextView) twoLineListItem.getChildAt(0);
                TextView textView2 = (TextView) twoLineListItem.getChildAt(1);
                textView1.setText(String.format("%s %s", member.getFirstName(), member.getLastName()));
                textView2.setText(String.format("%s", member.getEmail()));

                if (position % 2 == 0) {
                    twoLineListItem.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    twoLineListItem.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.chrome));
                }
                if (position == 0) {
                    twoLineListItem.setGravity(Gravity.BOTTOM);
                    textView1.setGravity(Gravity.BOTTOM);
                    textView1.setPadding(textView1.getPaddingLeft(), textView1.getTotalPaddingTop(), textView1.getPaddingRight(), 0);
                    textView1.setHeight(textView1.getLineHeight() + textView2.getLineHeight() / 2);
                    textView2.setHeight(textView2.getLineHeight() / 2);
                }

                return twoLineListItem;
            }
        };

        final JotformMemberViewModel jotformMemberViewModel = JotformMemberViewModel.getModel(this,
                UrlUtils.getMembersUrl(getContext()),
                UrlUtils.updateMembersUrl(getContext()),
                UrlUtils.addMembersUrl(getContext()));
        jotformMemberViewModel.getJotformMembersOrFetch(getActivity()).observe(this.getViewLifecycleOwner(), new Observer<Set<Member>>() {
            @Override
            public void onChanged(Set<Member> members) {
                if (getContext() == null || members == null || members.isEmpty()) {
                    return;
                }
                Set<Member> filteredMembers = new HashSet<>();
                for (Member member : members) {
                    if (member.getImage() == null) {
                        filteredMembers.add(member);
                    }
                }
                adapter.addAll(filteredMembers);
                adapter.sort(Member.getSortComparator());
                adapter.insert(new Member(null, null, getString(R.string.MEMBRE_EXISTANT_INCOMPLET), "", "", "", null), 0);
                adapter.notifyDataSetChanged();
            }
        });

        // Set up the buttons
        builder.setPositiveButton(R.string.ADD, (dialog, which) -> {
            if (getActivity() == null) {
                return;
            }
            Member member;
            if (spinner.getSelectedItemPosition() <= 0) {
                String first = inputFirstName.getText().toString().trim();
                String lastName = inputLastName.getText().toString().trim();
                String email = inputEmail.getText().toString().trim();
                String phone = inputPhone.getText().toString().trim();

                boolean is_valid = InputValidator.isMemberValid(
                        inputFirstName.getText().toString(),
                        inputLastName.getText().toString(),
                        inputEmail.getText().toString(),
                        inputPhone.getText().toString()
                );
                if (!is_valid) {
                    //TODO ABT
                    RuntimeException re = new RuntimeException(String.format("Unexpected: spinner.selectedItemPosition=%s, first=%s, last=%s, email=%s, phone=%s",
                            spinner.getSelectedItemPosition(), first, lastName, email, phone));
                    MyApplication.saveError(getContext(), re);
                    Toast.makeText(getActivity().getApplicationContext(), "Erreur en ajoutant le membre", Toast.LENGTH_SHORT).show();
                    return;
                }


                member = new Member(null, null,
                        WordUtils.capitalizeFully(first),
                        WordUtils.capitalizeFully(lastName),
                        email,
                        phone,
                        null
                );
                Toast.makeText(getActivity().getApplicationContext(), R.string.ONE_MEMBER_ADDED, Toast.LENGTH_SHORT).show();
            } else {
                member = (Member) spinner.getSelectedItem();
                if (member == null) {
                    //TODO ABT
                    String first = inputFirstName.getText().toString().trim();
                    String lastName = inputLastName.getText().toString().trim();
                    String email = inputEmail.getText().toString().trim();
                    String phone = inputPhone.getText().toString().trim();
                    RuntimeException re = new RuntimeException(String.format("Unexpected: spinner.selectedItemPosition=%s, first=%s, last=%s, email=%s, phone=%s, member=%s",
                            spinner.getSelectedItemPosition(), first, lastName, email, phone, member));
                    MyApplication.saveError(getContext(), re);
                    Toast.makeText(getActivity().getApplicationContext(), "Erreur en ajoutant le membre", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getActivity().getApplicationContext(), R.string.ONE_MEMBER_COMPLETED, Toast.LENGTH_SHORT).show();
            }
            //Toast.makeText(context, inputFirstName.getText().toString(), Toast.LENGTH_SHORT).show();

            //Create and Initialize new object with Face embeddings and Name.
            member.setImage(embeedings);

            jotformMemberViewModel.addMember(getActivity(), member);
            //MemberPersistence.addMember(getActivity(), member);
            //MemberPersistence.markMemberChanged(getActivity());
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (getActivity() == null) {
                    return;
                }
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                navController.navigate(R.id.navigation_members);
            }
        });
        builder.setNegativeButton(R.string.CANCEL, (dialog, which) -> {
            dialog.cancel();
        });

        dialog = builder.create();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);

        dialog.show();
        previewView.setVisibility(View.INVISIBLE);

        spinner = dialog.findViewById(R.id.existingMembers);
        inputFirstName = dialog.findViewById(R.id.firstName);
        inputLastName = dialog.findViewById(R.id.lastName);
        inputEmail = dialog.findViewById(R.id.email);
        inputPhone = dialog.findViewById(R.id.phone);

        spinner.setAdapter(adapter);
        prefilled = dialog.findViewById(R.id.prefilled);
        inputPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        inputPhone.setKeyListener(DigitsKeyListener.getInstance(getString(R.string.PHONE_DIGITS)));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        TextWatcher te = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean is_valid = InputValidator.isMemberValid(
                        inputFirstName.getText().toString(),
                        inputLastName.getText().toString(),
                        inputEmail.getText().toString(),
                        inputPhone.getText().toString()
                );

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(is_valid);
            }
        };
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i > 0) {
                    Member member = (Member) adapterView.getSelectedItem();
                    inputEmail.setText(member.getEmail());
                    inputFirstName.setText(member.getFirstName());
                    inputLastName.setText(member.getLastName());
                    inputPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
                    inputPhone.setKeyListener(DigitsKeyListener.getInstance("()- 0123456789"));
                    inputPhone.setText(FormatUtil.formatPhone(member.getPhone()));

                    inputEmail.setEnabled(false);
                    inputFirstName.setEnabled(false);
                    inputLastName.setEnabled(false);
                    inputPhone.setEnabled(false);
                    prefilled.setVisibility(View.INVISIBLE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    inputEmail.setEnabled(true);
                    inputFirstName.setEnabled(true);
                    inputLastName.setEnabled(true);
                    inputPhone.setEnabled(true);
                    inputEmail.setText("");
                    inputFirstName.setText("");
                    inputLastName.setText("");
                    inputPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                    inputPhone.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                    inputPhone.setText("");
                    prefilled.setVisibility(View.VISIBLE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        inputEmail.addTextChangedListener(te);
        inputLastName.addTextChangedListener(te);
        inputFirstName.addTextChangedListener(te);
        inputPhone.addTextChangedListener(te);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (getActivity() == null || getContext() == null) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                if (dialog != null && dialog.isShowing()) {
                    return;
                }
                Toast.makeText(getContext(), "On activity result select picture", Toast.LENGTH_LONG).show();
                Uri selectedImageUri = data.getData();
                try {

                    InputImage impphoto = InputImage.fromBitmap(ImageUtils.getBitmapFromUri(getActivity(), selectedImageUri), 0);
                    detector.process(impphoto).addOnSuccessListener(faces -> {
                        if (getActivity() == null || getContext() == null) {
                            return;
                        }
                        if (faces.size() != 0) {

                            Face face = faces.get(0);
//                              System.out.println(face);

                            //write code to recreate bitmap from source
                            //Write code to show bitmap to canvas

                            Bitmap frame_bmp = null;
                            try {
                                frame_bmp = ImageUtils.getBitmapFromUri(getActivity(), selectedImageUri);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Bitmap frame_bmp1 = ImageUtils.rotateBitmap(frame_bmp, 0, flipX, false);

                            //face_preview.setImageBitmap(frame_bmp1);


                            RectF boundingBox = new RectF(face.getBoundingBox());


                            Bitmap cropped_face = ImageUtils.getCropBitmapByCPU(frame_bmp1, boundingBox);

                            Bitmap scaled = ImageUtils.getResizedBitmap(cropped_face, 112, 112);
                            face_preview.setImageBitmap(scaled);

                            recognizeImage(scaled);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to add", Toast.LENGTH_SHORT).show();
                    });
                    face_preview.setImageBitmap(ImageUtils.getBitmapFromUri(getActivity(), selectedImageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
