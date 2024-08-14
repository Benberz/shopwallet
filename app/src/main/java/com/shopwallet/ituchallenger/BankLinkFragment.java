package com.shopwallet.ituchallenger;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.DocumentReference;

/**
 * Fragment that handles the linking of a bank account.
 * This fragment allows the user to select a bank and enter an account number to link their bank account.
 */
public class BankLinkFragment extends DialogFragment {

    private AutoCompleteTextView autoCompleteBankName;
    private EditText accountNumberEditText;
    private TextView linkStatus;
    private ImageView linkCheckMark;
    private ProgressBar linkProgress;

    private final String[] bankNames = {"Bank A", "Bank B", "Bank C"}; // Example list of bank names

    private BankLinkCallback bankLinkCallback;
    private String holderRefId; // Stores the reference ID for linking the bank account

    /**
     * Callback interface to notify when the bank account has been successfully linked.
     */
    public interface BankLinkCallback {
        void onBankLinked(DocumentReference documentReference);
    }

    /**
     * Sets the callback to be used when the bank account is linked.
     *
     * @param callback The callback to notify on bank linking success.
     */
    public void setBankLinkCallback(BankLinkCallback callback) {
        this.bankLinkCallback = callback;
    }

    /**
     * Creates a new instance of BankLinkFragment with the specified holderRefId.
     *
     * @param holderRefId The reference ID used for linking the bank account.
     * @return A new instance of BankLinkFragment.
     */
    public static BankLinkFragment newInstance(String holderRefId) {
        BankLinkFragment fragment = new BankLinkFragment();
        Bundle args = new Bundle();
        args.putString("holderRefId", holderRefId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve holderRefId from arguments
        if (getArguments() != null) {
            holderRefId = getArguments().getString("holderRefId");
        }
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogLinkBankTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bank_link_fragment, container, false);

        // Initialize UI elements
        autoCompleteBankName = view.findViewById(R.id.autoCompleteBankName);
        accountNumberEditText = view.findViewById(R.id.accountNumberEditText);
        linkStatus = view.findViewById(R.id.linkedStatusTextView);
        linkCheckMark = view.findViewById(R.id.linkedCheckimageView);
        linkProgress = view.findViewById(R.id.linkedBankProgressBar);
        Button linkToWalletButton = view.findViewById(R.id.linkToWalletButton);

        // Set up autocomplete for bank names
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, bankNames);
        autoCompleteBankName.setAdapter(adapter);

        // Set onClick listener for the link button
        linkToWalletButton.setOnClickListener(v -> linkToWallet());

        return view;
    }

    /**
     * Handles the process of linking the bank account.
     * Validates inputs and calls the HolderBankAccount service to link the bank.
     */
    private void linkToWallet() {
        String bankName = autoCompleteBankName.getText().toString().trim();
        String accountNumber = accountNumberEditText.getText().toString().trim();

        if (validateInputs(bankName, accountNumber)) {
            linkStatus.setText(R.string.processing_status_text);
            linkCheckMark.setVisibility(View.INVISIBLE);
            linkProgress.setVisibility(View.VISIBLE);

            // Call the HolderBankAccount service to link bank
            HolderBankAccount holderBankAccount = new HolderBankAccount();
            holderBankAccount.linkBank(holderRefId, bankName, accountNumber, new HolderBankAccount.BankLinkCallback() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    // Update UI for success
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Bank linked successfully", Toast.LENGTH_SHORT).show();
                        linkStatus.setVisibility(View.VISIBLE);
                        linkStatus.setText(R.string.linked_string);
                        linkCheckMark.setVisibility(View.VISIBLE);
                        linkProgress.setVisibility(View.GONE);

                        // Notify the activity that the bank has been linked
                        if (bankLinkCallback != null) {
                            bankLinkCallback.onBankLinked(documentReference);
                        }

                        // Close the dialog after a delay
                        new Handler().postDelayed(() -> dismiss(), 1000);
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Update UI for failure
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to link bank: " + errorMessage, Toast.LENGTH_SHORT).show();
                        linkStatus.setVisibility(View.VISIBLE);
                        linkStatus.setText(R.string.failed_text);
                        // Change linkCheckMark icon to failure icon
                        linkCheckMark.setVisibility(View.VISIBLE);
                        linkCheckMark.setImageResource(R.drawable.ic_failure);
                        linkProgress.setVisibility(View.GONE);
                    });
                }
            });
        }
    }

    /**
     * Validates the user inputs for bank name and account number.
     *
     * @param bankName       The name of the bank.
     * @param accountNumber  The account number to be linked.
     * @return True if inputs are valid, otherwise false.
     */
    private boolean validateInputs(String bankName, String accountNumber) {
        if (bankName.isEmpty()) {
            autoCompleteBankName.setError("Bank name is required");
            return false;
        }

        if (accountNumber.length() != 10) {
            accountNumberEditText.setError("Enter a valid 10-digit account number");
            return false;
        }

        return true;
    }
}