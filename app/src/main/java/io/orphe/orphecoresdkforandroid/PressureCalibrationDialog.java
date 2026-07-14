package io.orphe.orphecoresdkforandroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import io.orphe.orphecoresdk.OrpheInsolePressureCalibration;
import io.orphe.orphecoresdk.OrpheInsolePressureCoefficient;

/**
 * 6点の圧力補正値を編集するサンプルダイアログです。
 */
final class PressureCalibrationDialog {
    interface Listener {
        void onSave(@NonNull OrpheInsolePressureCalibration calibration);

        void onReset();
    }

    private PressureCalibrationDialog() {
    }

    static void show(
            @NonNull Context context,
            @NonNull String title,
            @NonNull OrpheInsolePressureCalibration initialCalibration,
            @NonNull Listener listener
    ) {
        final View content = LayoutInflater.from(context).inflate(
                R.layout.dialog_pressure_calibration,
                null,
                false
        );
        final EditText[] coefficient1Fields = findFields(content, new int[]{
                R.id.input_toe_inside_c1,
                R.id.input_mid_inside_c1,
                R.id.input_toe_outside_c1,
                R.id.input_center_c1,
                R.id.input_mid_outside_c1,
                R.id.input_heel_c1
        });
        final EditText[] coefficient2Fields = findFields(content, new int[]{
                R.id.input_toe_inside_c2,
                R.id.input_mid_inside_c2,
                R.id.input_toe_outside_c2,
                R.id.input_center_c2,
                R.id.input_mid_outside_c2,
                R.id.input_heel_c2
        });
        final EditText[] coefficient3Fields = findFields(content, new int[]{
                R.id.input_toe_inside_c3,
                R.id.input_mid_inside_c3,
                R.id.input_toe_outside_c3,
                R.id.input_center_c3,
                R.id.input_mid_outside_c3,
                R.id.input_heel_c3
        });
        final EditText[] thresholdFields = findFields(content, new int[]{
                R.id.input_toe_inside_threshold,
                R.id.input_mid_inside_threshold,
                R.id.input_toe_outside_threshold,
                R.id.input_center_threshold,
                R.id.input_mid_outside_threshold,
                R.id.input_heel_threshold
        });
        populate(
                coefficient1Fields,
                coefficient2Fields,
                coefficient3Fields,
                thresholdFields,
                initialCalibration
        );

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset to defaults", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                final OrpheInsolePressureCalibration calibration = readCalibration(
                        coefficient1Fields,
                        coefficient2Fields,
                        coefficient3Fields,
                        thresholdFields
                );
                if (calibration == null) {
                    return;
                }
                listener.onSave(calibration);
                dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                listener.onReset();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    @NonNull
    private static EditText[] findFields(@NonNull View content, int[] ids) {
        final EditText[] fields = new EditText[ids.length];
        for (int index = 0; index < ids.length; index++) {
            fields[index] = content.findViewById(ids[index]);
        }
        return fields;
    }

    private static void populate(
            @NonNull EditText[] coefficient1Fields,
            @NonNull EditText[] coefficient2Fields,
            @NonNull EditText[] coefficient3Fields,
            @NonNull EditText[] thresholdFields,
            @NonNull OrpheInsolePressureCalibration calibration
    ) {
        final OrpheInsolePressureCoefficient[] coefficients = {
                calibration.toeInside,
                calibration.midInside,
                calibration.toeOutside,
                calibration.center,
                calibration.midOutside,
                calibration.heel
        };
        for (int index = 0; index < coefficients.length; index++) {
            coefficient1Fields[index].setText(Double.toString(coefficients[index].coefficient1));
            coefficient2Fields[index].setText(Double.toString(coefficients[index].coefficient2));
            coefficient3Fields[index].setText(Double.toString(coefficients[index].coefficient3));
            thresholdFields[index].setText(Double.toString(coefficients[index].threshold));
        }
    }

    private static OrpheInsolePressureCalibration readCalibration(
            @NonNull EditText[] coefficient1Fields,
            @NonNull EditText[] coefficient2Fields,
            @NonNull EditText[] coefficient3Fields,
            @NonNull EditText[] thresholdFields
    ) {
        final OrpheInsolePressureCoefficient[] coefficients =
                new OrpheInsolePressureCoefficient[coefficient1Fields.length];
        boolean valid = true;
        for (int index = 0; index < coefficients.length; index++) {
            final Double coefficient1 = readFiniteDouble(coefficient1Fields[index]);
            final Double coefficient2 = readFiniteDouble(coefficient2Fields[index]);
            final Double coefficient3 = readFiniteDouble(coefficient3Fields[index]);
            final Double threshold = readFiniteDouble(thresholdFields[index]);
            if (coefficient1 == null
                    || coefficient2 == null
                    || coefficient3 == null
                    || threshold == null) {
                valid = false;
            } else {
                coefficients[index] = new OrpheInsolePressureCoefficient(
                        coefficient1,
                        coefficient2,
                        coefficient3,
                        threshold
                );
            }
        }
        if (!valid) {
            return null;
        }
        return new OrpheInsolePressureCalibration(
                coefficients[0],
                coefficients[1],
                coefficients[2],
                coefficients[3],
                coefficients[4],
                coefficients[5]
        );
    }

    private static Double readFiniteDouble(@NonNull EditText field) {
        final String rawValue = field.getText().toString().trim();
        if (rawValue.isEmpty()) {
            field.setError("Required");
            return null;
        }
        try {
            final double value = Double.parseDouble(rawValue);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                field.setError("Enter a finite number");
                return null;
            }
            field.setError(null);
            return value;
        } catch (NumberFormatException exception) {
            field.setError("Enter a valid number");
            return null;
        }
    }
}
