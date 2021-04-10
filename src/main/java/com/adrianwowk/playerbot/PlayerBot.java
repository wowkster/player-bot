package com.adrianwowk.playerbot;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class PlayerBot extends JavaPlugin implements Listener {

    public static HashMap<UUID, EntityPlayer> players = new HashMap<>();
    private static ItemStack pickaxeItem = new ItemStack(Material.DIAMOND_PICKAXE);
    private static Map<String, Map<String, Integer>> armorMap = new HashMap<>();
    private static Map<String, Integer> armorToughnessMap = new HashMap<>();
    private static Map<UUID, Boolean> invCooldown = new HashMap<>();
    public static Map<UUID, Inventory> invMap = new HashMap<>();

    public static final Property BOT1_SKIN =  new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjYzNjczNDA0MCwKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kM2Y4MDRhY2E1MjVhYjEwYWI1ZmFhNDA3OGI2MWQ0Y2IzYTM3YmU1OTdlOTRhOTA5ZmQxNGY3MzUzOTRiMDIiCiAgICB9CiAgfQp9", "W9F7E7Jh3CJoezdxSC4tVifz22XfmLPxbsaydaCmW9K/cLOBbnIDaF7Mh1FBQdlq3upseEYH6J2tMQ+e7AEfgEmuw333ySbex9TXJfYlI8tasnC3eNd+FFHCKPlOw5oT9Oe8k523jEnDBkrrdW/1rExuMh0YLxN3blyeF3Ukq2mz8qY8DEc8lkLI0ND9URtCJbzm1vrBlrePUr+e/gB1PLS5EoP0+zla0AdB9jI+d9PyGwsOjh1RwbMgM7MhpJcmwQuuQeHY8QgqgrfseOjL78/v1igqaQ7+fHv8mj6xeQ5Zmrs+3M2plwaKBe8DJ5YftdGMFGIR/huavttscc4NI5KLLCsCmPxO6zQMJWYLdRCukD2nt/nAlGtnGSRHZdaSMablrfMAtExaVbQkpA18SUSUGUKx1uuN1X59yONZspJJZ88mMWqqYg5b63RHvjODDP2mspHestcieW2MKUYiAeC4EOE+8nUJSEeTAQFIOMZW2gYl0cFqatm2Tp45BxkxieVJhVRb9tAK/gUvzJrTqAhnm9pWF/P1aMZYwFqiuTEGeLzBv9ZVMjwYTqU5ciqgtg7yA9Rr3ySGfmR8WlHgHwuHDDPg6UooDvzb85wp0EV6ZcMw+JmM1GYNvJEr1tq+Xsk0l90x4OdwwvuTyCjQ1my9QWrbT4zmSB/0Q6BOURI=");
    public static final Property BOT2_SKIN =  new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjYzNjczNDA0MCwKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kM2Y4MDRhY2E1MjVhYjEwYWI1ZmFhNDA3OGI2MWQ0Y2IzYTM3YmU1OTdlOTRhOTA5ZmQxNGY3MzUzOTRiMDIiCiAgICB9CiAgfQp9","W9F7E7Jh3CJoezdxSC4tVifz22XfmLPxbsaydaCmW9K/cLOBbnIDaF7Mh1FBQdlq3upseEYH6J2tMQ+e7AEfgEmuw333ySbex9TXJfYlI8tasnC3eNd+FFHCKPlOw5oT9Oe8k523jEnDBkrrdW/1rExuMh0YLxN3blyeF3Ukq2mz8qY8DEc8lkLI0ND9URtCJbzm1vrBlrePUr+e/gB1PLS5EoP0+zla0AdB9jI+d9PyGwsOjh1RwbMgM7MhpJcmwQuuQeHY8QgqgrfseOjL78/v1igqaQ7+fHv8mj6xeQ5Zmrs+3M2plwaKBe8DJ5YftdGMFGIR/huavttscc4NI5KLLCsCmPxO6zQMJWYLdRCukD2nt/nAlGtnGSRHZdaSMablrfMAtExaVbQkpA18SUSUGUKx1uuN1X59yONZspJJZ88mMWqqYg5b63RHvjODDP2mspHestcieW2MKUYiAeC4EOE+8nUJSEeTAQFIOMZW2gYl0cFqatm2Tp45BxkxieVJhVRb9tAK/gUvzJrTqAhnm9pWF/P1aMZYwFqiuTEGeLzBv9ZVMjwYTqU5ciqgtg7yA9Rr3ySGfmR8WlHgHwuHDDPg6UooDvzb85wp0EV6ZcMw+JmM1GYNvJEr1tq+Xsk0l90x4OdwwvuTyCjQ1my9QWrbT4zmSB/0Q6BOURI=");
    public static final Property BOT3_SKIN =  new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjgwNjczNjgxMiwKICAicHJvZmlsZUlkIiA6ICIyNzc1MmQ2ZTUyYmM0MzVjYmNhOWQ5NzY1MjQ2YWNhNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJkZW1pbWVkIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2U5MDYzZWQ0NzAxNzllNWQyNGVlYTRlMzZhYTliYmViNjMyZmE4ZWZmNWNlM2Y4MWIzMTU2ODRkMzMxNjU0NTYiCiAgICB9CiAgfQp9", "ChO/4e0e9egorxqeuyMXStXdTdYDtnOY14yh6pBqL8uDoq7WTf2LdBp98AGIojTYs+LQtgkTMqaFbsT4pNVIvqeBA5P/eYv/9SFScx+y6O3RQyPgkEmShyAtYU+mrIV+X6p+xOuQUT67yUFHYipjkNIdmuu5ARsg0eSU9druo5putre8usJDQFWD9zeLTb8nTkGkZKD6y66RzVSRgTVRbbeXF+tTJwGMiX3R05dCTwh0Fvjdtez0zqzZ02L0v2ILZgblg64kAG0N35sBeYEap+LostvDOGTBqLPEhpNS2H/V0bOvbv6wOGeXxd5lRUKEIT4+qhbBtsV0uq/NeY848P7/TZezVBZJSrYt08U84jMQp36vCNwKycR3UH0Rj6OcDxrXmd1Ynx1OPgx5bN84R+k28hOhNhsUDLcLn4N2GVvqQhmXu0wLaJptD3irnXf7DUySeXx1O7RfOeCuMPAqNZcDw2YU/SOZIoThkLUMoQdWiL5mfyHTiFyXBlpgP/AVMFvFDbPfFSeXnrE+txrWrzk/kcWWzd6mq3p82/iC2uUfRO7XzkFdK15sbrXRHdEynx5ToB0BCuIfPpyxu8QZ9xf2lJuJJCW3ad2XaZuVqImwG8hSd6QsRTuAqjh9Bs6/Ib8bR71ZiIqn99Rci6yQ5OTvrFdbCFB6FtwcHHzqcE8=");
    public static final Property ORANGE_SKIN =  new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjc3MTUzNTM2MiwKICAicHJvZmlsZUlkIiA6ICI5ZDQyNWFiOGFmZjg0MGU1OWM3NzUzZjc5Mjg5YjMyZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUb21wa2luNDIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg2YmZjNmU3MjIxODk0ZmQ0YjVlMWIzNWMzNjcxNTRhOGFlMzJhZmQ1YjVhOTExNTkxNzAwYTRkNzI4MzE1IgogICAgfQogIH0KfQ==", "ARcmnniYyBT+SM1b+sgwBMrM8jBmzHG5ocru1chXWnL1XFSAVob1NV7x3J+e2AkamWdMuQhe6qaZbKjkZ4lf64herb/nRGm3HuJi8aJbpmQYqwhQcpr82ibuR5S5ozQHOPEjuaHqz3YRzJIu0hMky8QD/4fPAVN3lK1WBsApr8MfF82LMgCUWTvGgjo73Fby+In+ompuJRXZi7MsK4hhGmKe7WpAbUpH/CvEewtfrYuQaop90IvLXZVZDbkUeKoWqL13ptk4Jjzbq0cq7Sg8nxlZsPXMEt1rYq1a8VJalf1t51YSC6oe7Xhaj0/u7h7tZaw8N1JTv00g3+zOUONUEk+6MKjqE2BHteRTa6jWmMfr1qPId01V0ety1seKH3CzOIO8SJuv+ejT8fx8jZSGm6uBcPFPRmtgZhwP1nIB2lQDjWbKW/5kfHPS3d8/Hvf2OjgoBzmQbAiDvULhFU1zxqm6FMlCDY1kkrc1GW9mwVqZshgHXyyeJ8nWNLSOFn1YlHAblHTS5AJcp2hcv8t58rwdgKl9m6Z8r5zjWqF42zbmlnawXXSWmRUtd1dtqRhGzqun9s5XtmrZ/nvXEgMVAGxBfBPYdcSrgQAZkWkQXu6QcBty4CTxWqsmvsUCdbw9VEJ9CDsG9FNKR5VoB79WZv8BXep3KJIBiUivNOu1fyc=");
    public static final Property WOWKSTER_SKIN = new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjc3MDQ4NTE1OSwKICAicHJvZmlsZUlkIiA6ICJmZDQ3Y2I4YjgzNjQ0YmY3YWIyYmUxODZkYjI1ZmMwZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJDVUNGTDEyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJmNjgzYTVlMmJlYmFlYzBmODZkMDE2NjIzZDkwZmVhMjBjYWZmNDU5MzlkZmNmN2U3YTcxYzE3ZTYyMmY4ZTgiCiAgICB9CiAgfQp9", "rYLcWvpSQ552JHtHYNcDldax4OXjXSMzV859WzD4wIWyCVhOXDaDOeUD5cwajxA174RhZUPGTLidN0YXzcyx3myajpVMkKg/+ryc8N185hdlJjVM8K7N6FAJtqbCLm21XdazX256NuArbMdoJQuSgTQn/tIV1ZEChuiHu12gSgRMO5+erojzKAcG8eQCEI7QGMAc7ihDRQptHr7qukDtUhYaCTAiAHDC/2ytqqVUCwF1n78LpKyf+ip2hdsq0N0QRy8AreX5b7gCIbh4chB9Jou8b1l4zOLZQRTXysYrw/MxpxJgmGY5/bPbcNOySfjhMKJkmQ2P8tjiP7OHY2S+vZDqPWQvj/jfnHKouO82V01XXpGVo3+GI+xDbk3PMx5XPbgm5VuP9CCDBDX/43/T4oOCy+zOI/4PCZ9XP4OnOIp2+bYaxuygDSzxXKNE/q2N3er6JVot3ZEgBYPPm+zDvAnLqDxtZT2aLbLFUbMx7xgFXbeopnZvZcM7vkLvWFgPNmVqipgzQ0ZUaNO65aPbMGtCIXG3S2BNRrJJlBRqKah7hTD6F0TTmTO7WEiCX3n5cGM/W02AFeXLZ9tE3mqGRHnCkrR1WIfoYS0Ivq3mljy4cTAHuC31JuElQmDa2k9P4a+Xf7XzyUQalpQLvZDax8aj8sDEI8q0TsbDCge+P/c=");

    public static final Property ULTRON1_SKIN = new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjgwNDQ1MzkwMiwKICAicHJvZmlsZUlkIiA6ICI0ZWQ4MjMzNzFhMmU0YmI3YTVlYWJmY2ZmZGE4NDk1NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJGaXJlYnlyZDg4IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdlOGZiZTRmYjFmMjc3NmVmNzNlMmU2ZmRiNTFjOGVkMTQyZTU4ZjdiZWVkYjhjYmEzYzYwNTFmMmVhMjMzOTIiCiAgICB9CiAgfQp9", "yjbfEHtlbrgjNrPl8zrf0JIKm+9q2KkZSexqWWQNwK2P8VLZuec1Fjh5kwhaxn5D0ybyhIscOfWpPqB9eWPgWOWAZrVPpVczKNCuT5qu+6KmdF1320eBiFFZ76kwnGMBJlYtTbvY2duUqW3xw2TUa5+7Nn83l1pSEuuuFTb2khYWtvM8mnQzFAkrrbpuXOsKhnGZV1aJdOahzUKc0TYeR+ZoMer5Apzsbu64z2XgkGJ7gKAIrL5HOceDWy7MFJzouaDIf5Gvl+aQK7CBoomkGSSJYTi9TCDsyHMRiTOI983v9MRA+jKfhZdujv01kF4ZKOATmkM0fnFQjhmc38gNd6hcupjWAF/pcI7p3u5FrYG0t2RPHskQkrKxPIlELN9hlA5uEDgcdgR6vlT2R2nuFzl4Sim0FJc+KYe0kHoAPJjrKuXXWxcl46/9eY9r4Vm2rgvzkuHWc31V7ZEcjOhNYddLjxGPemqcNWh1OaZeXRF2CmNHKKEsT8vYhB1M13WV+bVM7x82uHcygI1QvDB/Bp/DKY9cERmqFE7u/uUDjDr6NK2RVoyS36FDTVOM17Due94xymNfxEYnB+HvTT+we1vrIITgkuJ0kKUMKht8WUjDTtRI6URw6vUHzl3opOvh+WnOPmqJaqavmeFOzjuwRy3OwgCkmkWxLlrcb5cg5U0=");
    public static final Property ULTRON2_SKIN = new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjgwNDU4NDYxMSwKICAicHJvZmlsZUlkIiA6ICIzZjM4YmViZGYwMWQ0MjNkYWI4MjczZjUwNGFiNGEyNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJjazM0Nzk0MjM1NzUzNzMxIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzNjN2JmMTA3Y2UzMGJhNjQ4YzVkYzdkYjFlOGE1NzZiNjQ2NWY0YTBmNjEyOGEyZWE0ZTU1Yzg0OTg4NTJlN2EiCiAgICB9CiAgfQp9", "FmO/W8Mnb56vbA0WogaOP2Ayb2jL4vLMxTI93J/eQsn3h2Mp/t89g+c2OlJirhRi/hGpmyGSLpCOYBgZlS+SnPQljE0Xpi4kUJVMEBseGu6mrSyeyreG1bj2ikPMdMpAgwqdcNAEJISNAldikZt6cDRUvdeYN9RfIgiQzputzQWb3/04V/quswnEwxhW5uiGVfr4TGyvik/MNITyzJKHMIYoyH4B3btMb5CPN/FfPLH8mBO9vOQUsVKEXhygT8gQN2Us/XkiRiWV1CTsmdGMHVB425AcD89M8p+vsi5fIS/W2j0yYmyyEUkquIvmL4yowf8LVzCitl6WVmMNVoFdp9kFB4GdcoqI+7ultadzRNNmq8t/Vpxcltl17cPFux1PnrqnvEzYj6sYGoHNQL4LHGLIFK0wGtNumnpN5cvaqf9mEl3tXYZrjJiiclDcu/6JzyOnbygwEaJjYk4cE9i05ouW/qcdG7Uv5Q5mtP7pGnifZMZjXE1/8/vicW4XpB3LQAbRy4Q6bXaM2d/ZZre0dgScpWOKeyaO5l9lRADu8k6pZe3Mcxw4MMeJXsRXW7NTjaXZyWjSTseQs4ZOjko9Amv14K15CfJttqNJlT73bvFKchyf0PjmQFznkMFBE9vD5t1QJjoijRV6cKrQ9HFfn8PXCvvabmAsIbVCEkPBeQI=");
    public static final Property OOGA_BOOGA_SKIN = new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjgwNDYyOTAzMSwKICAicHJvZmlsZUlkIiA6ICJjZGM5MzQ0NDAzODM0ZDdkYmRmOWUyMmVjZmM5MzBiZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJSYXdMb2JzdGVycyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NDBlYzE5Yjc3ZWNmNDg3N2IyMzQ0ZDBmYTM0NDdkY2E3NjRlMzU0ZjAyMWZhMTZiOTRkZDIyNTk0Y2JmZTliIgogICAgfQogIH0KfQ==", "SJBe38YW3N5HpWf2b3MmXQhzbzpQ1NWcEPeMmPwLKqK7eY1byXokPCtcgFEZkXeFVEr+TaNBfTF+WLiegClfFGN0TVJYcDrPDVPtElNO2AS3zbzCVOxVxF59jpyOApjoVVZf7HoX6b59Qz8N8+jjFw/WZVoBOow8dbVbNUQU3GOxz9dA0+p5CidAeFScFbnX7wNJTn+d0ION+uccn10nLJQDog9EdTdJ97jSyC+hxtzGZolesSqJx6gWXz9Vb519uLX9RitRlrYCWZAcx7Oz4KR9wPXAWBMzDih2kzMRaltTencbHoYZYee9Gv6l0fqkvnHZzbApZI/h34+LzzipFlJviuMTMiCgpNMBGSTUcvvTLu/GUv+UU+Qr4tkUkKXqg3AbO67+JkWfxnvChjcJLFqy0PE6l8gtgRyVgnLzXzoNptYYb65PmIF3/4zU5Vtyt3zIGf12hI+nqm7CNwZFPEQ8kiPDT58MThefilUaydYpI5akRp/HLXeUPH1WgZ6/4SGDQ/2KniMVpzOsCmM0uBs8ypYWGXjG8dbin4tfRtUgXrV5vmtUOXMgGDjst/E9fjF3hkZG/6CPByF45BQC9HBNzDaRM2WDUAyv62z2RaBTbiw6utZN55JGv/MaHn6mOZVmkJbZI3Thox32F6dblwVPt1Tu+Pu3UXrlsvh7mN8=");
    public static final Property DOGE_BOT_SKIN = new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYxNjgwNDY3OTE1NCwKICAicHJvZmlsZUlkIiA6ICIwYTUzMDU0MTM4YWI0YjIyOTVhMGNlZmJiMGU4MmFkYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJQX0hpc2lybyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84YTgzMTFlNTllYTMwZGJhNDJjZTNkODJkYjU3M2IyMjJiYTk0YzU1YTJmNTMxZGMzMzY4Njk1ZmJmNmU2MGYwIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "uBXZQrUoFVTFupRQAATRnFabZCFh6UZ7UZnRusNTDhSntDe5fqU788w8Vhnyu04+4N4LIYO+dlTl/I2jZ0X2rCgWNRAKUi2zWgmADP8xLN3yyxWowIgQiP8EiVvhbtjutFYU+TX2a8pX9cguE8xGPo5PwfkG5V+ypoDtrAxAPycVm+fGKD6nXGbUzNUw2/ZeQtLYdaVT8LEg5eoIi91cof7lh1uBuRszoD7alhT/9qm22DyW5ahOFdGFfIeIFfAEbACJmQ4Tc7XwwuO9ZYdDS0TmWLcWmwEpKxl7y6ujV8Tw8Uu3FaGzFL2FBaT/JBoclfbo0H9O7NUwJV23wfgFpTDnMFRm0m4TTRNTDxd/nQSLOPCvHCciQZkdVTv3YltXwBtxsTYKz+B6aka5Wjs4XgA31s0uc/1oEgnG6Sz2Ny+aAvYbrEIHM2c7GhcBe7mRqGl3J//MpbQgtrTqckc5PWhoiTyVxeFyiAbCv2H8gza9xp91b3gcmySCx8lABZcct465qhIZUAsuo2GmY2f7cEB+YfYwx+nhYmCb1rrMoV0I5ibKbrx9jcRb0MZyT5Tb5z7HjiJkD2Qj21cnEYqVxTvF9Yht4QUFHCu+UXtI5AkeRlEBxqgn/OGI5ddxAUubRpm0soL4O8GNMmEpcKiB65ucCwnnxMsy1Mq5KUj+lmY=");
    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);

        pickaxeItem.addEnchantment(Enchantment.DIG_SPEED, 5);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("bot"))
            return false;
        if (!(sender instanceof Player)){
            sender.sendMessage("§cYou must be a player to use this command");
            return true;
        }

        Player p = (Player) sender;
        Location loc = p.getLocation();
        int preferredYValue = (int) loc.getY();

        sender.sendMessage("§eTrying to spawn PlayerEntity");

        WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();

        UUID uuid = UUID.randomUUID();

        int rot = (args.length == 0 ? 0 : Integer.parseInt(args[0]));

        GameProfile profile = new GameProfile(uuid, "Bot" + rot );// (int) (Math.random() * 1000));
        profile.getProperties().put("textures", BOT3_SKIN);

        EntityBot bot = new EntityBot(rot, loc, world.getMinecraftServer(), world, profile, new PlayerInteractManager(world.getMinecraftWorld()));

        new BukkitRunnable() {
            public void run() {
                if (bot.onTick()){
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0l, 1l);

        // Send Packets to all connected players
        for (Player player : Bukkit.getOnlinePlayers()){
            CraftPlayer cp = (CraftPlayer)player;

            cp.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, bot));
            cp.getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(bot));
            cp.getHandle().playerConnection.sendPacket(getEquipmentPacket(bot));
            cp.getHandle().playerConnection.sendPacket(bot.entityMetadataPacket);
        }

        players.put(uuid, bot);

        sender.sendMessage("§d§lSpawned Bot with preferred y value of: " + preferredYValue);

        return true;
    }

    @EventHandler
    public void clickEntity(PlayerInteractEntityEvent event){
        if (!(event.getRightClicked() instanceof LivingEntity))
            return;
        LivingEntity le = (LivingEntity) event.getRightClicked();
        if (!(le instanceof Player) || !le.hasMetadata("NPC"))
            return;
        Player p = (Player) le;
        invCooldown.put(p.getUniqueId(), true);
        p.setMetadata("Interacting", new FixedMetadataValue(PlayerBot.getPlugin(PlayerBot.class), true));
        Inventory inv =  Bukkit.createInventory(p, 54, "Bot Inventory");
        invMap.put(p.getUniqueId(), inv);
        for (int i = 0; i < p.getInventory().getContents().length; i++){
            ItemStack item = p.getInventory().getContents()[i];
            item = (item == null ? new ItemStack(Material.GRAY_STAINED_GLASS_PANE) : item);
//            ItemMeta meta = item.getItemMeta();
//            meta.setDisplayName("" + i);
//            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        for (int i = 41; i < 54; i++){
            ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        event.getPlayer().openInventory(inv);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            invCooldown.put(p.getUniqueId(), false);
            if (!invMap.get(p.getUniqueId()).getViewers().contains(event.getPlayer())){
                p.removeMetadata("Interacting", this);
            }
        }, 20l);
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event){
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof Player))
            return;
        Player p = (Player)event.getInventory().getHolder();
        if (p.hasMetadata("NPC") && !invCooldown.getOrDefault(p.getUniqueId(), true)){
            p.removeMetadata("Interacting", this);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event){
        LivingEntity le = event.getEntity();

        if (!(le instanceof Player) || !le.hasMetadata("NPC"))
            return;

        if (event.getItem().hasMetadata("Armor")) {
            event.setCancelled(true);
            return;
        }

        Player p = (Player) le;
        double prot = protPercent(event.getItem().getItemStack());

        Material mat = event.getItem().getItemStack().getType();

//        if (mat == Material.COBBLESTONE || mat == Material.GRANITE || mat == Material.DIORITE || mat == Material.ANDESITE){
//            new BukkitRunnable() {
//                public void run() {
//                    p.getInventory().remove(event.getItem().getItemStack());
//                    Item item = p.getWorld().dropItemNaturally(p.getLocation(), event.getItem().getItemStack());
//                    item.setMetadata("Armor", new FixedMetadataValue(PlayerBot.getPlugin(PlayerBot.class), true));
//                }
//            }.runTaskLater(this, 1l);
//        }

        String[] parts = event.getItem().getItemStack().getType().toString().split("_");
        if (parts.length != 2) {
            return;
        }
        String type = parts[1];

        if (type.equalsIgnoreCase("HELMET")){
            double helmProt = protPercent(p.getInventory().getHelmet());
            if (prot > helmProt){
                ItemStack itemToDrop = (p.getInventory().getHelmet() == null ? new ItemStack(Material.AIR) : p.getInventory().getHelmet());
                if (itemToDrop.getType() != Material.AIR)
                    p.getWorld().dropItemNaturally(p.getLocation(), itemToDrop);
                p.getInventory().setHelmet(event.getItem().getItemStack());
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    p.getInventory().remove(event.getItem().getItemStack());
                }, 1l);
            } else {
                new BukkitRunnable() {
                    public void run() {
                        p.getInventory().remove(event.getItem().getItemStack());
                        Item item = p.getWorld().dropItemNaturally(p.getLocation(), event.getItem().getItemStack());
                        item.setMetadata("Armor", new FixedMetadataValue(PlayerBot.getPlugin(PlayerBot.class), true));
                    }
                }.runTaskLater(this, 1l);
            }
        } else if (type.equalsIgnoreCase("CHESTPLATE")){
            double chestProt = protPercent(p.getInventory().getChestplate());
            if (prot > chestProt){
                ItemStack itemToDrop = (p.getInventory().getChestplate() == null ? new ItemStack(Material.AIR) : p.getInventory().getChestplate());
                if (itemToDrop.getType() != Material.AIR)
                    p.getWorld().dropItemNaturally(p.getLocation(), itemToDrop);
                p.getInventory().setChestplate(event.getItem().getItemStack());
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    p.getInventory().remove(event.getItem().getItemStack());
                }, 1l);
            } else {
                new BukkitRunnable() {
                    public void run() {
                        p.getInventory().remove(event.getItem().getItemStack());
                        Item item = p.getWorld().dropItemNaturally(p.getLocation(), event.getItem().getItemStack());
                        item.setMetadata("Armor", new FixedMetadataValue(PlayerBot.getPlugin(PlayerBot.class), true));
                    }
                }.runTaskLater(this, 1l);
            }
        } else if (type.equalsIgnoreCase("LEGGINGS")){
            double legProt = protPercent(p.getInventory().getLeggings());
            if (prot > legProt){
                ItemStack itemToDrop = (p.getInventory().getLeggings() == null ? new ItemStack(Material.AIR) : p.getInventory().getLeggings());
                if (itemToDrop.getType() != Material.AIR)
                    p.getWorld().dropItemNaturally(p.getLocation(), itemToDrop);
                p.getInventory().setLeggings(event.getItem().getItemStack());
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    p.getInventory().remove(event.getItem().getItemStack());
                }, 1l);
            } else {
                new BukkitRunnable() {
                    public void run() {
                        p.getInventory().remove(event.getItem().getItemStack());
                        Item item = p.getWorld().dropItemNaturally(p.getLocation(), event.getItem().getItemStack());
                        item.setMetadata("Armor", new FixedMetadataValue(PlayerBot.getPlugin(PlayerBot.class), true));
                    }
                }.runTaskLater(this, 1l);
            }
        } else if (type.equalsIgnoreCase("BOOTS")){
            double bootProt = protPercent(p.getInventory().getBoots());
            if (prot > bootProt){
                ItemStack itemToDrop = (p.getInventory().getBoots() == null ? new ItemStack(Material.AIR) : p.getInventory().getBoots());
                if (itemToDrop.getType() != Material.AIR)
                    p.getWorld().dropItemNaturally(p.getLocation(), itemToDrop);
                p.getInventory().setBoots(event.getItem().getItemStack());
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    p.getInventory().remove(event.getItem().getItemStack());
                }, 1l);
            } else {
                new BukkitRunnable() {
                    public void run() {
                        p.getInventory().remove(event.getItem().getItemStack());
                        Item item = p.getWorld().dropItemNaturally(p.getLocation(), event.getItem().getItemStack());
                        item.setMetadata("Armor", new FixedMetadataValue(PlayerBot.getPlugin(PlayerBot.class), true));
                    }
                }.runTaskLater(this, 1l);
            }
        } else {

        }

        for (Player p_ : Bukkit.getOnlinePlayers())
            ((CraftPlayer)p_).getHandle().playerConnection.sendPacket( getEquipmentPacket( ((CraftPlayer) p).getHandle()) );
    }

    public double protPercent(ItemStack item){
        if (item == null)
            return 0;

        int protLevel = item.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);

        String[] parts = item.getType().toString().split("_");
        if (parts.length != 2)
            return 0;
        String tier = parts[0];
        String type = parts[1];
        Integer defensePoints;
        Integer toughnessPoints;
        try {
            defensePoints = armorMap.get(type).get(tier);
            defensePoints = (defensePoints != null ? defensePoints : 0);

            toughnessPoints = armorToughnessMap.get(tier);
            toughnessPoints = (toughnessPoints != null ? toughnessPoints : 0);
        } catch (NullPointerException npe){
            return 0;
        }
        return 100D * ((Math.min(20d, Math.max(defensePoints / 5d, defensePoints - (4d / (toughnessPoints + 8d))))) / 25d) * (1 + (4 * protLevel * 0.01));
//        return (4 * defensePoints) * (1 + (5 * protLevel * 0.01));
    }

    /**
     * Remove the player from tab list when they are killed
     * @param e
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        Player p = e.getEntity();
        EntityPlayer ep = ((CraftPlayer)p).getHandle();

        if (!p.hasMetadata("NPC"))
            return;

        UUID uuid = ep.getUniqueID();
        players.remove(uuid);
        for (ItemStack item : ep.getBukkitEntity().getInventory().getContents()) {
            if (item != null && !item.equals(ep.getBukkitEntity().getInventory().getItemInMainHand()))
                ep.getBukkitEntity().getWorld().dropItemNaturally(ep.getBukkitEntity().getLocation(), item);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            ((EntityBot) ep).resetProgress();
            ep.setLocation(0,-100,0, 0f, 0f);
            ep.getWorldServer().removePlayer(ep);
            ep.getBukkitEntity().disconnect("Bot");
            for (Player player : Bukkit.getOnlinePlayers()){
                PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;
                connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ep));
                connection.sendPacket(new PacketPlayOutEntityDestroy(ep.getId()));
            }
        }, 30l);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        CraftPlayer cp = (CraftPlayer)e.getPlayer();

        for (EntityPlayer entity : players.values()) {
            cp.getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entity));
            cp.getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(entity));
            cp.getHandle().playerConnection.sendPacket(getEquipmentPacket(entity));
        }
    }

    public PacketPlayOutEntityEquipment getEquipmentPacket(EntityPlayer entity){
        // Create Equipment Packet List
        final List<Pair<EnumItemSlot, net.minecraft.server.v1_16_R3.ItemStack>> equipmentList = new ArrayList<>();

        // Add equipment to Packet
        equipmentList.add(new Pair<>(EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(entity.getBukkitEntity().getEquipment().getHelmet())));
        equipmentList.add(new Pair<>(EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(entity.getBukkitEntity().getEquipment().getChestplate())));
        equipmentList.add(new Pair<>(EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(entity.getBukkitEntity().getEquipment().getLeggings())));
        equipmentList.add(new Pair<>(EnumItemSlot.FEET, CraftItemStack.asNMSCopy(entity.getBukkitEntity().getEquipment().getBoots())));

        equipmentList.add(new Pair<>(EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(entity.getBukkitEntity().getItemInHand())));
        equipmentList.add(new Pair<>(EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(entity.getBukkitEntity().getInventory().getItemInOffHand())));

        // Creating the packet
        final PacketPlayOutEntityEquipment entityEquipment = new PacketPlayOutEntityEquipment(entity.getId(), equipmentList);

        return entityEquipment;
    }

    static {

        armorMap.put("HELMET", new HashMap<String, Integer>(){{
            put("LEATHER", 1);
            put("CHAINMAIL", 2);
            put("GOLDEN", 2);
            put("IRON", 2);
            put("DIAMOND", 3);
            put("NETHERITE", 3);
        }});
        armorMap.put("CHESTPLATE", new HashMap<String, Integer>(){{
            put("LEATHER", 3);
            put("CHAINMAIL", 5);
            put("GOLDEN", 5);
            put("IRON", 6);
            put("DIAMOND", 8);
            put("NETHERITE", 8);
        }});
        armorMap.put("LEGGINGS", new HashMap<String, Integer>(){{
            put("LEATHER", 2);
            put("CHAINMAIL", 3);
            put("GOLDEN", 4);
            put("IRON", 5);
            put("DIAMOND", 6);
            put("NETHERITE", 6);
        }});
        armorMap.put("BOOTS", new HashMap<String, Integer>(){{
            put("LEATHER", 1);
            put("CHAINMAIL", 1);
            put("GOLDEN", 1);
            put("IRON", 2);
            put("DIAMOND", 3);
            put("NETHERITE", 3);
        }});

        armorToughnessMap.put("DIAMOND", 2);
        armorToughnessMap.put("NETHERITE", 3);

    }
}
