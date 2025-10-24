package com.lypaka.battletower.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um disco personalizado no TM-Factory (TM, TR ou HM).
 * A lista {@code discs} será preenchida na inicialização do plugin.
 */
public class CustomDisc {

    /** Lista global de discos lidos da config. */
    public static final List<CustomDisc> discs = new ArrayList<>();

    private final String name;       // Nome do golpe  (ex.: "Flamethrower")
    private final String discType;   // "TM", "TR"…
    private final String enumType;   // Tipo do golpe (ex.: "FIRE")

    public CustomDisc(String name, String discType, String enumType) {
        this.name      = name;
        this.discType  = discType;
        this.enumType  = enumType;
    }

    /* ---------------- getters ---------------- */
    public String getName()      { return name;     }
    public String getDiscType()  { return discType; }
    public String getEnumType()  { return enumType; }

    /* ---------------- utilidade de carga ---------------- */
    /** Exemplo de carga a partir da config – ajuste ao seu formato real. */
    public static void loadFromConfig() {
        // Percorra sua config e faça:
        // discs.add(new CustomDisc(moveName, "TM", "FIRE"));
    }
}
