package net.milkbowl.vault.placeholder.children;

import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.models.SuffixEntry;
import net.milkbowl.vault.placeholder.VaultPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EconomyHook {

    private final VaultPlaceholder parent;

    private char decimalSeparator;
    private NumberFormat commasFormat;
    private final Map<Integer, NumberFormat> decimalFormatsCache;

    private final SuffixEntry[] suffixes;

    private final Economy economy;

    public EconomyHook(VaultPlaceholder expansion) {
        this.parent = expansion;
        this.decimalFormatsCache = new Object2ObjectOpenHashMap<>();
        this.suffixes = new SuffixEntry[5];
        this.economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
    }

    @Nullable
    public String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        final double balance = this.getBalance(player);

        if (params.length() > 8 && Character.isDigit(params.charAt(8)) && params.endsWith("dp")) {
            final String decimalPlaces = params.substring(8, params.length() - 2);
            final Integer points = Ints.tryParse(decimalPlaces);
            if (points == null) return "'" + decimalPlaces + "' is not a valid number";
            return this.setDecimalPoints(balance, points);
        }

        return switch (params) {
            case "balance" -> this.setDecimalPoints(balance, Math.max(2, this.economy.fractionalDigits()));
            case "balance_fixed" -> String.valueOf(Math.round(balance));
            case "balance_formatted" -> this.formatBalance((long) balance);
            case "balance_commas" -> this.commasFormat.format(balance);
            default -> null;
        };
    }

    private double getBalance(@NotNull OfflinePlayer player) {
        return this.economy.getBalance(player);
    }

    @NotNull
    private String setDecimalPoints(double balance, int points) {
        if (points < 0) points = 0;

        return this.decimalFormatsCache.computeIfAbsent(points, key -> {
            final DecimalFormat format = new DecimalFormat();
            format.setMaximumFractionDigits(key);
            format.setMinimumFractionDigits(0);
            format.setGroupingUsed(false);
            return format;
        }).format(balance);
    }

    @NotNull
    private String formatBalance(long balance) {
        if (balance == Long.MIN_VALUE) return this.formatBalance(Long.MIN_VALUE + 1);
        if (balance < 0) return "-" + this.formatBalance(-balance);
        if (balance < 1000) return Long.toString(balance);

        final SuffixEntry suffixEntry = this.getSuffixEntry(balance);

        final long truncated = balance / (suffixEntry.value() / 10);
        final boolean hasDecimal = truncated < 100 && truncated % 10 != 0;

        if (hasDecimal) {
            return (truncated / 10) + this.decimalSeparator + (truncated % 10) + suffixEntry.suffix();
        } else {
            return (truncated / 10) + suffixEntry.suffix();
        }
    }

    private SuffixEntry getSuffixEntry(long balance) {
        if (balance < 1_000_000L) {
            return this.suffixes[4];
        } else if (balance < 1_000_000_000L) {
            return this.suffixes[3];
        } else if (balance < 1_000_000_000_000L) {
            return this.suffixes[2];
        } else if (balance < 1_000_000_000_000_000L) {
            return this.suffixes[1];
        }

        return this.suffixes[0];
    }

    public void setup() {
        final ConfigurationSection formattingSection = this.parent.getConfigSection("formatting");
        Objects.requireNonNull(formattingSection);

        final boolean usNumberFormat = formattingSection.getBoolean("us-number-format");
        this.decimalSeparator = usNumberFormat ? '.' : ',';
        this.commasFormat = NumberFormat.getInstance(usNumberFormat ? Locale.ENGLISH : Locale.GERMAN);

        this.suffixes[0] = new SuffixEntry(1_000_000_000_000_000L, formattingSection.getString("quadrillions", "Q"));
        this.suffixes[1] = new SuffixEntry(1_000_000_000_000L, formattingSection.getString("trillions", "T"));
        this.suffixes[2] = new SuffixEntry(1_000_000_000L, formattingSection.getString("billions", "B"));
        this.suffixes[3] = new SuffixEntry(1_000_000L, formattingSection.getString("millions", "M"));
        this.suffixes[4] = new SuffixEntry(1_000L, formattingSection.getString("thousands", "K"));
    }
}
