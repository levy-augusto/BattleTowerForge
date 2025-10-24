# 🏰 Battle Tower (Pixelmon Reforged 8.4.3)
> Plugin desenvolvido por **Levy / AGP-Responged Team**  
> Compatível com **Minecraft 1.12.2 + Pixelmon Reforged 8.4.3 + SpongeForge 7.3.0**  
> Atualizado em **Outubro de 2025**

---

## 📚 Sumário
1. [⚙️ Instalação](#️-instalação)
2. [🧱 Estrutura da Torre](#-estrutura-da-torre)
3. [🧭 Montando a Battle Tower](#-montando-a-battle-tower)
4. [⚔️ Fluxo de Jogo (Jogadores)](#️-fluxo-de-jogo-jogadores)
5. [🔒 Sistema de Limite (Quota & Cooldown)](#-sistema-de-limite-quota--cooldown)
6. [🧑‍💼 Comandos Administrativos](#-comandos-administrativos)
7. [💬 Mensagens Customizáveis](#-mensagens-customizáveis)
8. [🧩 Integração com LuckPerms](#-integração-com-luckperms)
9. [🧰 Debug e Logs](#-debug-e-logs)
10. [🧱 Estrutura dos Arquivos](#-estrutura-dos-arquivos)
11. [🏁 Roadmap](#-roadmap)
12. [📜 Licença](#-licença)
13. [💬 Créditos](#-créditos)

---

## ⚙️ Instalação

1. Coloque o `.jar` da Battle Tower em:
   ```
   ./mods/
   ```
   ou
   ```
   ./mods/plugins/
   ```
   (dependendo se você usa Forge puro ou SpongeForge).

2. Inicie o servidor uma vez.  
   Será criada a pasta:
   ```
   ./config/battletower/
   ```

3. Dentro dela estarão os arquivos de configuração:
   ```
   battletower.conf
   random.conf
   shop.conf
   locations.conf
   messages.conf
   ```

---

## 🧱 Estrutura da Torre

A Battle Tower funciona com **três tipos principais de NPCs:**

| NPC | Função | Local de registro |
|------|--------|-------------------|
| 🧍 **Receptionist** | Inicia desafios, checa quota e cooldown | `Receptionist-Locations` |
| ⚔️ **Trainer** | Treinadores dos andares intermediários | `Trainer-Locations` |
| 👑 **Head Boss** | Chefe final de cada Tier | `Head-Boss-Location` |

Cada NPC é registrado com coordenadas exatas (mundo, X, Y, Z).

---

## 🧭 Montando a Battle Tower

1. **Construa o prédio da torre** (ex: `/warp battletower`).
2. Posicione os NPCs nos andares desejados.
3. Use os comandos para registrá-los:

```bash
/bt setreceptionist         # Define o NPC clicado como recepcionista
/bt settrainer <tier>       # Define o NPC clicado como treinador do tier N
/bt setboss <tier>          # Define o NPC clicado como chefe final do tier N
```

4. Crie arenas de batalha com:
```bash
/bt setroom <tier> <room>
```

5. Verifique tudo:
```bash
/bt locations
```

---

## ⚔️ Fluxo de Jogo (Jogadores)

1. O jogador interage com o **Receptionist**.
2. Se tiver quota disponível, é aberta a GUI de **seleção de time**.
3. O jogador escolhe até 6 Pokémon válidos.
4. Ao confirmar:
   - O sistema checa quota e cooldown.
   - Se permitido, a batalha começa.
   - Ao vencer, o jogador ganha **BP (Battle Points)** e sobe de tier.
5. Caso perca, pode tentar novamente conforme os limites definidos.

---

## 🔒 Sistema de Limite (Quota & Cooldown)

### 📊 Configuração (`battletower.conf`)

```hocon
Tower {
  Challenge-Quota {
    Enabled = true
    Mode = daily
    Attempts-Default = 1
    Daily-Reset = "03:00"
    Duration = "24h"
    Per-Tier = false
    Timezone = "America/Sao_Paulo"
    Message-When-Limited = "&cVocê já fez seu desafio. Volte em {time_left}."

    From-Permissions {
      Meta-Key = bt-quota
      Permission-Prefix = "battletower.quota."
      Use-Meta-First = true
    }
  }

  # Cooldown global após esgotar quota
  challenge-cooldown = 60 # minutos
}
```

### 🧠 Funcionamento

- Cada jogador tem um número fixo de tentativas por janela (configurável).
- O limite pode ser definido via:
  - **Meta do LuckPerms:** `bt-quota=3`
  - **Perm Node:** `battletower.quota.3`
- Quando o limite é atingido, inicia-se um cooldown configurável.
- O reset ocorre automaticamente conforme `Mode`:
  - `daily`: em hora fixa (ex: 03:00)
  - `duration`: após X horas (ex: 6h, 24h, etc.)

---

## 🧑‍💼 Comandos Administrativos

| Comando | Permissão | Descrição |
|----------|------------|-----------|
| `/bt` | `battletower.admin` | Mostra o menu de ajuda. |
| `/bt setreceptionist` | `battletower.admin` | Define o NPC clicado como recepcionista. |
| `/bt settrainer <tier>` | `battletower.admin` | Define o NPC clicado como treinador do tier especificado. |
| `/bt setboss <tier>` | `battletower.admin` | Define o NPC clicado como chefe final do tier. |
| `/bt setroom <tier> <room>` | `battletower.admin` | Salva a posição atual como arena do tier. |
| `/bt locations` | `battletower.admin` | Lista todas as localizações registradas. |
| `/bt reload` | `battletower.admin` | Recarrega todas as configurações. |
| `/bt quota <player>` | `battletower.admin` | Mostra as tentativas restantes e o tempo até o reset *(planejado)* |
| `/bt resetquota <player>` | `battletower.admin` | Reseta manualmente a quota de um jogador *(planejado)* |

---

## 💬 Mensagens Customizáveis

Arquivo: `messages.conf`

```hocon
NPC-Messages {
  Introduction = [
    "&eBem-vindo à Battle Tower!",
    "&7Escolha seu time e prove seu valor!"
  ]
  Challenge-Made = [
    "&aBoa sorte no seu desafio!",
    "&7Mostre sua força, treinador!"
  ]
  Victory = [
    "&6Parabéns! Você venceu o desafio!"
  ]
  Defeat = [
    "&cVocê perdeu, mas pode tentar novamente!"
  ]
}
```

---

## 🧩 Integração com LuckPerms

| Tipo | Exemplo | Efeito |
|------|----------|--------|
| 🧠 **Meta** | `/lp user Levy meta set bt-quota 3` | Permite 3 desafios por janela |
| 🔑 **Perm Node** | `/lp user Levy permission set battletower.quota.3 true` | Alternativa via permissão |
| ⚙️ **Prioridade** | `"Use-Meta-First = true"` | Faz a meta ter prioridade sobre o node |

---

## 🧰 Debug e Logs

Prefixos no console:

| Prefixo | Descrição |
|----------|------------|
| `[BT-ShopDebug]` | Eventos de clique em NPCs |
| `[BT-QuotaDebug]` | Sistema de quotas |
| `[BT-TierDebug]` | Progresso e batalhas |
| `[BT-CooldownDebug]` | Cooldown entre desafios *(em breve)* |

Ative logs detalhados no `battletower.conf`:
```hocon
Debug = true
```

---

## 🧱 Estrutura dos Arquivos

```
/config/battletower/
├── battletower.conf      # Configurações principais (Tower, Quota, Cooldown)
├── locations.conf        # NPCs e arenas registradas
├── messages.conf         # Mensagens e diálogos
├── random.conf           # Treinadores randômicos
└── shop.conf             # Lojas de BP
```

---

## 🏁 Roadmap

| Funcionalidade | Status |
|----------------|--------|
| Cooldown global configurável | 🔄 Em andamento |
| BP Rewards integrados | ⚙️ Em desenvolvimento |
| Comandos `/bt quota` e `/bt resetquota` | 🧱 Planejado |
| GUI de status do jogador (BP + Tier) | 🧩 Planejado |
| Reset diário automático | 🕓 Planejado |

---

## 📜 Licença

Uso livre em servidores Pixelmon Reforged, desde que mantido o crédito a **Levy (AGP-Responged Team)**.  
É proibida a revenda sem autorização prévia.

---

## 💬 Créditos

- 👑 **Levy Augusto Gomes Silva** — Developer principal  
- 🔧 **AGP-Responged Framework** — Base do sistema de integração  
- 🧱 **Pixelmon Reforged Team** — API e hooks de batalha  
- ❤️ Contribuidores e testadores da comunidade Pixelmon BR

---
