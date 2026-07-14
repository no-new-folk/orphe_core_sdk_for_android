package io.orphe.orphecoresdkforandroid;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MeasurementDetailActivity extends AppCompatActivity {
    public static final String EXTRA_MEASUREMENT_ID = "measurement_id";

    private MeasurementStorage mMeasurementStorage;
    private MeasurementRecord mRecord;
    private boolean mPendingSaveLeft;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);

    private TextView mTitleTextView;
    private TextView mSummaryTextView;
    private TextView mLeftPreviewTextView;
    private TextView mRightPreviewTextView;
    private Button mShareButton;
    private Button mSaveLeftButton;
    private Button mSaveRightButton;
    private Button mDeleteButton;

    private final ActivityResultLauncher<String> mCreateCsvDocument =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri == null || mRecord == null) {
                    return;
                }
                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    if (outputStream == null) {
                        throw new IllegalStateException("OutputStream is null.");
                    }
                    mMeasurementStorage.copyCsvTo(mRecord, mPendingSaveLeft, outputStream);
                    Toast.makeText(this, "CSVを保存しました", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "CSVの保存に失敗しました", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_detail);

        mMeasurementStorage = new MeasurementStorage(this);
        mTitleTextView = findViewById(R.id.text_detail_title);
        mSummaryTextView = findViewById(R.id.text_detail_summary);
        mLeftPreviewTextView = findViewById(R.id.text_left_csv_preview);
        mRightPreviewTextView = findViewById(R.id.text_right_csv_preview);
        mShareButton = findViewById(R.id.button_detail_share);
        mSaveLeftButton = findViewById(R.id.button_save_left_csv);
        mSaveRightButton = findViewById(R.id.button_save_right_csv);
        mDeleteButton = findViewById(R.id.button_detail_delete);

        String id = getIntent().getStringExtra(EXTRA_MEASUREMENT_ID);
        mRecord = id == null ? null : mMeasurementStorage.findRecord(id);
        if (mRecord == null) {
            Toast.makeText(this, "計測データが見つかりません", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindRecord();
        mShareButton.setOnClickListener(v -> shareCsv());
        mSaveLeftButton.setOnClickListener(v -> saveCsv(true));
        mSaveRightButton.setOnClickListener(v -> saveCsv(false));
        mDeleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void bindRecord() {
        mTitleTextView.setText(mDateFormat.format(new Date(mRecord.startTimeMillis)));
        mSummaryTextView.setText(String.format(
                Locale.US,
                "Duration: %.1fs\nLeft: %d / Right: %d\n%s / %s",
                mRecord.durationMillis() / 1000.0,
                mRecord.leftCount,
                mRecord.rightCount,
                mRecord.receiveMode,
                mRecord.samplingRate
        ));

        mLeftPreviewTextView.setText(readPreview(true));
        mRightPreviewTextView.setText(readPreview(false));
        mSaveLeftButton.setEnabled(mMeasurementStorage.getCsvFile(mRecord, true) != null);
        mSaveRightButton.setEnabled(mMeasurementStorage.getCsvFile(mRecord, false) != null);
        if (!mSaveLeftButton.isEnabled()) {
            mSaveLeftButton.setTextColor(Color.rgb(117, 117, 117));
        }
        if (!mSaveRightButton.isEnabled()) {
            mSaveRightButton.setTextColor(Color.rgb(117, 117, 117));
        }
    }

    private String readPreview(boolean left) {
        try {
            String preview = mMeasurementStorage.readCsvPreview(mRecord, left, 8);
            if (preview.length() == 0) {
                return left ? "No left CSV" : "No right CSV";
            }
            return preview;
        } catch (Exception e) {
            return "Failed to read CSV preview";
        }
    }

    private void shareCsv() {
        ArrayList<Uri> uris = new ArrayList<>(mMeasurementStorage.getCsvUris(mRecord));
        if (uris.isEmpty()) {
            Toast.makeText(this, "共有できるCSVがありません", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent;
        if (uris.size() == 1) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("text/csv");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share CSV"));
    }

    private void saveCsv(boolean left) {
        if (mMeasurementStorage.getCsvFile(mRecord, left) == null) {
            Toast.makeText(this, "CSVがありません", Toast.LENGTH_SHORT).show();
            return;
        }
        mPendingSaveLeft = left;
        String fileName = left ? "sensor-left.csv" : "sensor-right.csv";
        mCreateCsvDocument.launch(mRecord.id + "-" + fileName);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete measurement")
                .setMessage(mDateFormat.format(new Date(mRecord.startTimeMillis)) + " を削除しますか？")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (mMeasurementStorage.deleteRecord(mRecord.id)) {
                        Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "削除できませんでした", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
