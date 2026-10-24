package app.nepaliapp.bscnotes.dashboard.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import app.nepaliapp.bscnotes.R;

public class UnlockFragment extends Fragment {

    private int userCoins = 250; // Starting NotePass coins
    private TextView tvCoinBalance;
    private Button btnUnlockMonth, btnUnlockLifetime;

    public UnlockFragment() {
        // Required empty public constructor
    }

    public static UnlockFragment newInstance(String param1, String param2) {
        UnlockFragment fragment = new UnlockFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_unlock, container, false);

        // Bind UI views
        tvCoinBalance = view.findViewById(R.id.tvCoinBalance);
        btnUnlockMonth = view.findViewById(R.id.btnUnlockMonth);
        btnUnlockLifetime = view.findViewById(R.id.btnUnlockLifetime);

        // Update initial coin display
        updateCoinDisplay();

        // Handle 1 Month Pass Purchase
        btnUnlockMonth.setOnClickListener(v -> {
            int cost = 100;
            if (userCoins >= cost) {
                userCoins -= cost;
                updateCoinDisplay();
                Toast.makeText(requireContext(), "Successfully unlocked 1 Month NotePass!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Not enough NotePass coins!", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Lifetime Pass Purchase
        btnUnlockLifetime.setOnClickListener(v -> {
            int cost = 500;
            if (userCoins >= cost) {
                userCoins -= cost;
                updateCoinDisplay();
                Toast.makeText(requireContext(), "Successfully unlocked Lifetime Access!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Not enough NotePass coins! Earn more via streaks.", Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }

    private void updateCoinDisplay() {
        tvCoinBalance.setText("🪙 " + userCoins + " Coins");
    }
}