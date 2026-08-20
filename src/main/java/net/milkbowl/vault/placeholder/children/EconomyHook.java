package net.milkbowl.vault.placeholder.children;

import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.placeholder.VaultPlaceholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class EconomyHook {

    private final VaultPlaceholder parent;
    private final Economy economy;

    private String decimalSeparator;
    private NumberFormat commasFormat;
    private final Int2ObjectMap<NumberFormat> decimalFormatsCache;

    private String thousandsSuffix;
    private String millionsSuffix;
    private String billionsSuffix;
    private String trillionsSuffix;
    private String quadrillionsSuffix;

    public EconomyHook(VaultPlaceholder expansion, Economy economy) {
        this.parent = expansion;
        this.economy = economy;
        this.decimalFormatsCache = new Int2ObjectOpenHashMap<>();
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
            format.setMaximumFractionDigits(key); // max
            format.setMinimumFractionDigits(0); // min
            format.setGroupingUsed(false);
            return format;
        }).format(balance);
    }

    @NotNull
    private String formatBalance(long balance) {
        if (balance == Long.MIN_VALUE) return this.formatBalance(Long.MIN_VALUE + 1);
        if (balance < 0) return "-" + this.formatBalance(-balance);
        if (balance < 1000) return Long.toString(balance);

        final long divisor;
        final String suffix;

        if (balance >= 1_000_000_000_000_000L) {
            divisor = 1_000_000_000_000_000L;
            suffix = this.quadrillionsSuffix;
        } else if (balance >= 1_000_000_000_000L) {
            divisor = 1_000_000_000_000L;
            suffix = this.trillionsSuffix;
        } else if (balance >= 1_000_000_000L) {
            divisor = 1_000_000_000L;
            suffix = this.billionsSuffix;
        } else if (balance >= 1_000_000L) {
            divisor = 1_000_000L;
            suffix = this.millionsSuffix;
        } else {
            divisor = 1_000L;
            suffix = this.thousandsSuffix;
        }

        final long truncated = balance / (divisor / 10);
        final boolean hasDecimal = truncated < 100 && truncated % 10 != 0;

        if (hasDecimal) {
            return (truncated / 10) + this.decimalSeparator + (truncated % 10) + suffix;
        } else {
            return (truncated / 10) + suffix;
        }
    }

    public void setup() {
        final ConfigurationSection formattingSection = this.parent.getConfigSection("formatting");
        Objects.requireNonNull(formattingSection);

        final boolean usNumberFormat = formattingSection.getBoolean("us-number-format");
        this.decimalSeparator = usNumberFormat ? "." : ",";
        this.commasFormat = NumberFormat.getInstance(usNumberFormat ? Locale.ENGLISH : Locale.GERMAN);

        this.quadrillionsSuffix = formattingSection.getString("quadrillions", "Q");
        this.trillionsSuffix = formattingSection.getString("trillions", "T");
        this.billionsSuffix = formattingSection.getString("billions", "B");
        this.millionsSuffix = formattingSection.getString("millions", "M");
        this.thousandsSuffix = formattingSection.getString("thousands", "K");
    }
}
