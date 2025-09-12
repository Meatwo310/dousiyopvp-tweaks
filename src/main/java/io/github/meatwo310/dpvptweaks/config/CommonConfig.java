package io.github.meatwo310.dpvptweaks.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.meatwo310.dpvptweaks.command.Report;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.stream.Stream;

public class CommonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REPORTS = BUILDER
            .comment("Reports from players with reasons stored as a list of JSON strings.")
            .defineListAllowEmpty("reports", List.of(), CommonConfig::validateElement);

    private static boolean validateElement(Object obj) {
        if (!(obj instanceof String str)) {
            return false;
        }

        try {
            new Gson().fromJson(str, Report.class);
            return true;
        } catch (JsonSyntaxException ignored) {
            return false;
        }
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static void addReport(Report report) {
        var json = new Gson().toJson(report);
        List<String> list = Stream.concat(REPORTS.get().stream(), Stream.of(json)).toList();
        REPORTS.set(list);
    }
}
