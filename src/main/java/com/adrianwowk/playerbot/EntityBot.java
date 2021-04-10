package com.adrianwowk.playerbot;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class EntityBot extends EntityPlayer {
    public int preferredYValue;
    private ItemStack pickaxeItem;
    private Entity bukkit;
    private double progress;
    private int ticks = 0;
    private boolean jumping = false;
    private boolean placingJump = false;
    private BlockPosition lastBlockPos;
    private int angleOfRotaion;
    private int brokeKelp = 0;
    public PacketPlayOutEntityMetadata entityMetadataPacket;

    private static HashMap<net.minecraft.server.v1_16_R3.Material, Material> toolMap = new HashMap<>();

    public EntityBot(int rot, Location loc, MinecraftServer minecraftserver, WorldServer worldserver, GameProfile gameprofile, PlayerInteractManager playerinteractmanager) {
        super(minecraftserver, worldserver, gameprofile, playerinteractmanager);

        // Create Bukkit Entity
        // TODO - Check if this actually works xd
        this.bukkit = this.getBukkitEntity();

        // Create Default Pickaxe
        // TODO - Check if this is necessary
        this.pickaxeItem = new ItemStack(Material.DIAMOND_PICKAXE);
        this.pickaxeItem.addEnchantment(Enchantment.DIG_SPEED, 5);

        // Set some initial Values
        this.preferredYValue = (int) loc.getY(); // The y value it will try to mine at
        this.playerConnection = new PlayerConnection(minecraftserver, new NetworkManager(EnumProtocolDirection.SERVERBOUND), this); // Create a fake player connection
        this.angleOfRotaion = rot;
        this.setLocation(loc.getX(), loc.getY(), loc.getZ(), angleOfRotaion - 90f, 0f); // Set the initial location of the entity
        this.invulnerableTicks = 0; // Make the bot vulnerable right when spawning

        DataWatcher watcher = this.getDataWatcher();
        watcher.set(new DataWatcherObject<>(16, DataWatcherRegistry.a), (byte)255);
        this.entityMetadataPacket = new PacketPlayOutEntityMetadata(this.getId(), watcher, true);

        // Modify Generic Attributes
        this.getBukkitEntity().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(100);
        this.getBukkitEntity().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);
        this.getBukkitEntity().getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(20);
        this.getBukkitEntity().getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(12);
        this.getBukkitEntity().getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(20);

        // Identifiable Metadata
        this.getBukkitEntity().setMetadata("NPC", new FixedMetadataValue(PlayerBot.getPlugin(PlayerBot.class), true));

        // Set Equipment
       /* this.getBukkitEntity().getEquipment().setItemInMainHand(pickaxeItem);
        this.getBukkitEntity().getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));*/

        // Add player entity to world
        worldserver.addPlayerJoin(this);

        this.progress = 0;
    }

    /**
     * Execute this once per tick on the entity
     * @return Returns true if the entity has died and the runnable should be cancelled
     */
    public boolean onTick(){
        ticks++;
        if (!this.isAlive())
            return true;

        // If jumping, disable horizontal movement
        if (jumping){
            Vector vel = this.getBukkitEntity().getVelocity();
            vel.setX(0);
            vel.setZ(0);
            this.getBukkitEntity().setVelocity(vel);
        }

        // Do vanilla movement (Gravity, Knock back, etc.)
        this.movementTick();

//        if (!isOnGround() && jumping)
//            return false;

        // Send Packets to all players
        for (Player p_ : Bukkit.getOnlinePlayers()){
            PlayerConnection conn = ((CraftPlayer)p_).getHandle().playerConnection;
            conn.sendPacket( getEquipmentPacket(this) );
            conn.sendPacket(this.entityMetadataPacket);
//            conn.sendPacket(new PacketPlayOutEntity.PacketPlayOutEntityLook(this.getId(), (byte)(yaw * 256 / 360), (byte)(pitch * 256 / 360), true));
        }

        // Calculate Custom Gravity (Bhop)
        if (!this.isOnGround()){
            Vector vel = this.getBukkitEntity().getVelocity();
            if (!this.isOnGround() && jumping && vel.getY() < 0) {
                vel.add(new Vector(0, -3, 0));
                this.getBukkitEntity().setVelocity(vel);
            }
        } else {
            jumping = false;
        }

        if (brokeKelp > 0){
            brokeKelp--;
            return false;
        }

        // Stop here if a player is accessing the inventory
        if (this.getBukkitEntity().hasMetadata("Interacting"))
            return false;

        // Get the bukkit location because it's easier to use
        Location l = this.getBukkitEntity().getLocation();

        // Target Nearby Entities
        if (entityLogic(l))
            return false;

        if (rayCastOres(l))
            return false;

        // Center the bot in the Z direction
        if (centerParallelDirection())
            return false;

        // If jumping to place a block, don't continue
        if (placingJump)
            return false;

        // If standing in kelp, break it
        if (l.getBlock().getType().toString().contains("KELP")){
            breakBlock(l);
            return false;
        }

        // If standing in a liquid, jump up and place a block
        if (l.getBlock().isLiquid() || l.getBlock().getType().toString().contains("SEA")){
            // If the block above the bot's head is breakable break it with animations
            if (!isBadBlock(l.clone().add(0,2,0))){
                tryBreak(l.clone().add(0,2,0));
                return false;
            }
            jumpAndPlace(l);
            return false;
        }

        // If up against a wall (of unbreakable blocks) and it's possible to jump over it, jump and place a block
        if (isUnbreakable(rotateByAngle(l, 1, 1, 0)) || isUnbreakable(rotateByAngle(l, 1, 2, 0)) ){
            // If the block above the bot's head is breakable break it with animations
            if (!isBadBlock(l.clone().add(0, 2, 0))) {
                tryBreak(l.clone().add(0, 2, 0));
                return false;
            }
            if (!isUnbreakable(rotateByAngle(l, 0, 2, 0)))
                jumpAndPlace(l);
            return false;
        }

        // If block in front is in kelp, break it
        if (rotateByAngle(l, 1, -1, 0).getBlock().getType().toString().contains("KELP")){
            breakBlock(rotateByAngle(l, 1, -1, 0));
            brokeKelp = 2;
            return false;
        }

        // If the bot is standing on a solid block and the next block in front of it is a liquid, (scaffold)
        if (rotateByAngle(l, 0, -1, 0).getBlock().getType().isSolid()){
            // TODO - investigate sea grass
            if (rotateByAngle(l, 1, -1, 0).getBlock().isLiquid() || rotateByAngle(l, 1, -1, 0).getBlock().getType().toString().contains("SEA")){
                if (!isBadBlock(rotateByAngle(l, 1, 1, 0))){
                    tryBreak(rotateByAngle(l, 1, 1, 0));
                    return false;
                } else if (!isBadBlock(rotateByAngle(l, 1, 0, 0))){
                    tryBreak(rotateByAngle(l, 1, 0, 0));
                    return false;
                }
                // place block at l
                if (edgeX())
                    return false;
                placeBlock(rotateByAngle(l, 1, -1, 0));
                return false;
            }
        }

        // Do mining logic
        miningLogic(l);

        return false;
    }

    private void sendHeadPackets(){
//        if (ticks % 2 == 0) {
            for (Player p_ : Bukkit.getOnlinePlayers()) {
                PlayerConnection conn = ((CraftPlayer) p_).getHandle().playerConnection;
                conn.sendPacket(new PacketPlayOutEntityHeadRotation(this, (byte) (yaw * 256 / 360)));
                conn.sendPacket(new PacketPlayOutEntity.PacketPlayOutEntityLook(this.getId(), (byte) (yaw * 256 / 360), (byte) (pitch * 256 / 360), this.isOnGround()));
            }
//        }
    }

    private Location rotateByAngle(Location origLoc, double x, double y, double z){
        Vector rel = new Vector(x, y, z);

        rel.rotateAroundY( Math.toRadians(-angleOfRotaion) );

        return origLoc.toVector().add(rel).toLocation(origLoc.getWorld());
    }

    // TODO - Add this
    private void lookAt(Location l){
        if (l.getBlockZ() - (int)this.locZ() != 0 || l.getBlockX() - (int)this.locX() != 0)
            this.yaw = (float) Math.toDegrees(Math.atan2(l.getZ() - this.locZ(), l.getX() - this.locX())) - 90;
        this.pitch = (float) Math.toDegrees( Math.atan((this.locY() + 1) - l.getY() ) );
        sendHeadPackets();
    }

    /**
     * Looks for exposed ores using a ray trace, and mines them
     * @param l The bot location
     * @return Whether or not to return from the parent method
     */
    private boolean rayCastOres(Location l){
        // Initialize list of random vectors
        ArrayList<Vector> vecs = new ArrayList<>();
        for (int i = 0; i < 200; i++)
            vecs.add(new Vector( (Math.random() - 0.5), (Math.random() - 0.5), (Math.random() - 0.5) ));

        // For every random vector, create a trace and add it to the rays list
        ArrayList<RayTraceResult> rays = new ArrayList<>();
        for (Vector vec : vecs)
            rays.add(l.getWorld().rayTraceBlocks(l.clone().add(0,(Math.random() < 0.5 ? 1 : 0),0), vec, 5, FluidCollisionMode.ALWAYS, true));

        // Remove any null traces (Traces that didn't find a block)
        for (int i = rays.size() - 1; i >= 0; i--)
            if (rays.get(i) == null)
                rays.remove(i);

        // Sort the list based on an arbitrary metric (using coordinates)
        rays.sort(Comparator.comparingDouble(lel -> lel.getHitBlock().getX() * 2 + lel.getHitBlock().getY() * 4 + lel.getHitBlock().getZ() * 8));
        // Remove duplicate results using a helper method
        rays = removeDuplicates(rays);

        // If the block is an ore, try to mine it
        for (RayTraceResult res : rays){
            // TODO - if it finds debris, dig all around it
            if (res.getHitBlock().getType().toString().contains("ORE") || res.getHitBlock().getType().toString().contains("DEBRIS")) {
                if (!isBadBlock(res.getHitBlock().getLocation())){
                    tryBreak(res.getHitBlock().getLocation());
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Helper method to remove dulpicate ray traces from a List
     * @param l List of ray traces (none of them can be null iirc)
     * @return The new List without the duplicates
     */
    public ArrayList<RayTraceResult> removeDuplicates(List<RayTraceResult> l) {
        Set<Object> s = new TreeSet<>((o1, o2) -> {
            // ... compare the two object according to your requirements
            RayTraceResult res1 = (RayTraceResult) o1;
            RayTraceResult res2 = (RayTraceResult) o2;

            return (res1.getHitBlock().getLocation().equals(res2.getHitBlock().getLocation()) ? 0 : 1);
        });
        s.addAll(l);
        List<Object> res = Arrays.asList(s.toArray());

        ArrayList<RayTraceResult> result = new ArrayList<>();

        for (Object obj : res)
            result.add((RayTraceResult) obj);

        return result;
    }

    /**
     * Center the bot on the Z-Axis
     * @return Whether or not to return from the parent method (if the bot needed to move)
     */
    private boolean centerParallelDirection(){
        if (angleOfRotaion % 180 == 0) {
            double diffZ = Math.abs(this.locZ() - ((int) this.locZ()));

            if (diffZ > 0.6) {
                this.move(EnumMoveType.SELF, new Vec3D(0, 0, -(diffZ - 0.5)));
                return true;
            } else if (diffZ < 0.4) {
                this.move(EnumMoveType.SELF, new Vec3D(0, 0, Math.abs(diffZ - 0.5)));
                return true;
            }
        } else {
            double diffX = Math.abs(this.locX() - ((int) this.locX()));

            if (diffX > 0.6){
                this.move(EnumMoveType.SELF, new Vec3D(-(diffX - 0.5), 0, 0 ));
                return true;
            } else if (diffX < 0.4){
                this.move(EnumMoveType.SELF, new Vec3D(Math.abs(diffX - 0.5), 0, 0 ));
                return true;
            }
        }
        return false;
    }

    /**
     * Center the bot on the X-Axis
     * @return Whether or not to return from the parent method (if the bot needed to move)
     */
    public boolean centerPerpendicularDirection(){
        if (angleOfRotaion % 180 == 0) {
            double diffX = Math.abs(this.locX() - ((int) this.locX()));

            if (diffX > 0.6){
                this.move(EnumMoveType.SELF, new Vec3D(-(diffX - 0.5), 0, 0 ));
                return true;
            } else if (diffX < 0.4){
                this.move(EnumMoveType.SELF, new Vec3D(Math.abs(diffX - 0.5), 0, 0 ));
                return true;
            }
        } else {
            double diffZ = Math.abs(this.locZ() - ((int) this.locZ()));

            if (diffZ > 0.6) {
                this.move(EnumMoveType.SELF, new Vec3D(0, 0, -(diffZ - 0.5)));
                return true;
            } else if (diffZ < 0.4) {
                this.move(EnumMoveType.SELF, new Vec3D(0, 0, Math.abs(diffZ - 0.5)));
                return true;
            }
        }
        return false;
    }

    /**
     * Move the bot towards the edge of the block in the X direction
     * @return Whether or not to return from the parent method (if the bot needed to move)
     */
    public boolean edgeX(){
        // TODO - implement for all directions
//        if (angleOfRotaion % 180 == 0) {
//            double diffX = Math.abs(this.locX() - ((int) this.locX()));
//
//            if (diffX < 0.8) {
//                this.move(EnumMoveType.SELF, new Vec3D(Math.abs(diffX - 0.9), 0, 0));
//                return true;
//            }
//        }
        return false;
    }

    /**
     * Spoof a block placement at a location
     * @param l Location of block placement
     */
    private void placeBlock(Location l) {
        // Use trig to calculate the head direction dependent on the x and z location
        lookAt(l);

        l.getBlock().setType(Material.COBBLESTONE);
        this.getBukkitEntity().getInventory().setItemInMainHand(new ItemStack(Material.COBBLESTONE));

        // Send Packets
        for (Player player : Bukkit.getOnlinePlayers()) {
            CraftPlayer cp = (CraftPlayer) player;
            cp.getHandle().playerConnection.sendPacket(getEquipmentPacket(this)); // Equipment packet for blocks
            cp.getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(this, 0)); // Swing animation
            cp.playSound(this.getBukkitEntity().getLocation(), l.getBlock().getBlockData().getSoundGroup().getPlaceSound(), 1f, 1f); // Fake placement sound
        }
    }

    /**
     * Spoof a block break as is a player had broken it
     * @param l The location of the block to be broken
     */
    private void breakBlock(Location l){
        // Get the old id of the block
        int id = net.minecraft.server.v1_16_R3.Block.getCombinedId(((CraftBlockData) Bukkit.createBlockData(l.getBlock().getType())).getState().getBlock().getBlockData());

        // Break the block on the server using the tool in the bot's main hand
        l.getBlock().breakNaturally(this.getBukkitEntity().getInventory().getItemInMainHand());

        // Send packets
        for (Player player : Bukkit.getOnlinePlayers()){
            CraftPlayer cp = (CraftPlayer)player;
            cp.getHandle().playerConnection.sendPacket(new PacketPlayOutWorldEvent(2001, new BlockPosition(l.getX() , l.getY(), l.getZ()), id, false)); // Fake "Block Break" packet
            cp.getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(this, 0)); // Swing animation
        }
    }

    /**
     * Resets the breaking animation
     */
    public void resetProgress(){
        this.progress = 0;

        if (lastBlockPos == null)
            return;

        PacketPlayOutBlockBreakAnimation pac = new PacketPlayOutBlockBreakAnimation(this.getId(), lastBlockPos, 0);
        for (Player player : Bukkit.getOnlinePlayers())
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(pac);
    }

    /**
     * Progress a block break
     * @param l Location of the block to be broken
     */
    private void tryBreak(Location l){
        // Create BlockPos
        BlockPosition pos = new BlockPosition(l.getX() , l.getY(), l.getZ());
        this.lastBlockPos = pos;

        // Trig for head pitch
        lookAt(l);

        // Create break animation packet
        PacketPlayOutBlockBreakAnimation pac = new PacketPlayOutBlockBreakAnimation(this.getId(), pos, (int)(progress * 10));

        // Get right tool for the block to be broken
        net.minecraft.server.v1_16_R3.Material mat_ = ((CraftBlockData) Bukkit.createBlockData(l.getBlock().getType())).getState().getBlock().getBlockData().getMaterial();
        Material mat = toolMap.get(mat_); // Lookup the block material in the tool map

        // If the tool is null, make it air
        if (mat == null)
            mat = Material.AIR;

        // Set the tool material in the bot's main hand
        ItemStack item_ = this.getBukkitEntity().getEquipment().getItemInMainHand();
        item_.setType(mat);
        item_.addEnchantment(Enchantment.DIG_SPEED, 5);
        this.getBukkitEntity().getEquipment().setItemInMainHand(item_);

        // Send Packets
        for (Player player : Bukkit.getOnlinePlayers()) {
            CraftPlayer cp = (CraftPlayer) player;
            cp.getHandle().playerConnection.sendPacket(pac); // Break animation
//            cp.getHandle().playerConnection.sendPacket(getEquipmentPacket(this)); // Equipment
            cp.getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(this, 0)); // Swing
        }

        // Increase progress by an amount based on the block data and the tool data
        progress += (strengthVsBlock(this, this.getBukkitEntity().getInventory().getItemInMainHand(), l.getBlock()));

        // If the progress is over 1 (i.e. the block should be broken already), break the block
        if (progress >= 1D) {
            progress = 0D;
            breakBlock(l);
        }
    }

    /**
     * Try to jump and place a block beneath the bot
     * @param l The location to place the block
     */
    private void jumpAndPlace(Location l) {
        // If the bot is not on the ground, DO NOT jump
        if (this.isOnGround()) {
            // Only continue if the bot is centered on the block
            if (centerPerpendicularDirection())
                return;

            // Jump
            this.jump();
            placingJump = true; // Variable to limit horizontal movement while jumping

            // After 5 ticks (bot is in the air) place the block and allow horizontal vel
            Bukkit.getScheduler().runTaskLater(PlayerBot.getPlugin(PlayerBot.class), () -> {
                placingJump = false;
                placeBlock(l);
            }, 5L);
        }
    }

    /**
     * Finds nearby entities and does something depending on what they are
     * @param l The location to find entities
     * @return Whether or not to return from the parent method
     */
    // TODO - use a ray trace to not hit mobs through walls
    private boolean entityLogic(Location l){
        ArrayList<Vector> vecs = new ArrayList<>();
        for (int i = 0; i < 200; i++)
            vecs.add(new Vector( (Math.random() - 0.5), (Math.random() - 0.5), (Math.random() - 0.5) ));
        ArrayList<RayTraceResult> rays = new ArrayList<>();

        for (Vector vec : vecs){
            rays.add(l.getWorld().rayTraceEntities(l.clone().add(0,(Math.random() < 0.5 ? 1 : 0),0), vec, 5, (e) ->
                e.getType() == EntityType.FALLING_BLOCK
            ));
        }

        for (int i = rays.size() - 1; i >= 0; i--){
            if (rays.get(i) == null)
                rays.remove(i);
        }

        if (rays.size() > 0){
            return true;
        }

        // Get a list of all nearby entities
        ArrayList<Entity> ents = (ArrayList<Entity>) l.getWorld().getNearbyEntities(l, 6, 4, 6 );
        ents.sort((lhs, rhs) -> {
            // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
            double distanceL = lhs.getLocation().distance(l);
            double distanceR = rhs.getLocation().distance(l);
            return Double.compare(distanceR, distanceL);
        });
        Collections.reverse(ents);

        // Loop through ents
        for (Entity e : ents) {
            // If the entity is an Item and the bot's inventory is not full,
            // and the distance is between (1 - 4) then launch the item towards the bot
            if (e.getType() == EntityType.DROPPED_ITEM
                    && this.getBukkitEntity().getInventory().firstEmpty() != -1
                    && e.getLocation().distance(l) > 1
                    && e.getLocation().distance(l) <= 5) {
                // If the item was thrown by the bot, don't launch it
                if (e.hasMetadata("Armor"))
                    continue;

                // Do vector subtraction to get the desired velocity
                Vector epVec = l.toVector();
                Vector eVec = e.getLocation().toVector();

                e.setVelocity(epVec.subtract(eVec).normalize());

                // Make the bot able to pickup the item instantly by removing the delay
                org.bukkit.entity.Item item = (Item) e;
                item.setPickupDelay(0);
                if (!PlayerBot.players.containsKey(item.getOwner()))
                    item.setOwner(this.getUniqueID());
            }
            // Check if the entity is Living
            else if (e instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) e;
                EntityLiving el = ((CraftLivingEntity) le).getHandle();

                // If the entity is not the bot itself, the entity is still alive, and the entity is not in an I-Frame
                if (!el.equals(this) && el.isAlive() && el.noDamageTicks == 0) {
                    // Don't target players
                    if (el instanceof EntityPlayer || el.isInvulnerable())
                        continue;

                    // Set the bot's main hand to hold a sword
                    ItemStack item_ = this.getBukkitEntity().getEquipment().getItemInMainHand();
                    item_.setType(Material.DIAMOND_SWORD);
                    this.getBukkitEntity().getEquipment().setItemInMainHand(item_);

//                    // Jump if the bot is not in the air
//                    if (this.isOnGround()) {
//                        this.jump();
//                        // Delay jumping var by a tick so that it works
//                        // TODO - Investigate the correct delay for this
//                        Bukkit.getScheduler().runTaskLater(PlayerBot.getPlugin(PlayerBot.class), () -> {
//                            jumping = true;
//                        }, 2L);
//                    }

                    // Do some trig to calculate the angle needed to face the entity
                    lookAt(el.getBukkitEntity().getLocation());

                    // Actually attack the entity
                    this.attack(el);

                    // Send packets
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        CraftPlayer cp = (CraftPlayer) player;
                        cp.getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(this, 0)); // Swing animation packet
                        cp.getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(el, 4)); // Critical hit particle packet
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // TODO - document this
    private void miningLogic(Location l){

        Location l_ = null;

        if (!isBadBlock(l))
            l_ = l;
        else if (!isBadBlock(rotateByAngle(l, 0, 1, 0)))
            l_ = rotateByAngle(l, 0, 1, 0);
        else if (!isBadBlock(rotateByAngle(l, 1, 1, 0)))
            l_ = rotateByAngle(l, 1, 1, 0);
        else if ((int)this.locY() < preferredYValue){
            if (!isBadBlock(rotateByAngle(l, 1, 2, 0)))
                l_ = rotateByAngle(l, 1, 2, 0);
            else if (!isBadBlock(rotateByAngle(l, 1, 3, 0)) )
                l_ = rotateByAngle(l, 1, 3, 0);
            else if (!isBadBlock(rotateByAngle(l, 0, 2, 0)))
                l_ = rotateByAngle(l, 0, 2, 0);
            // If free to move, then do it
            else {
                if (!jumping) {
                    Vector v = new Vector(0.3, 0, 0).rotateAroundY(Math.toRadians(-angleOfRotaion));
                    this.move(EnumMoveType.SELF, new Vec3D(v.getX(), v.getY(), v.getZ()));
                }
                return;
            }
        }
        else if ((int)this.locY() > preferredYValue){
            if (rotateByAngle(l, 1, 0, 0).getBlock().isEmpty() &&
                    rotateByAngle(l, 1, -1, 0).getBlock().getType().isSolid() &&
                    (rotateByAngle(l, 1, -2, 0).getBlock().isLiquid() || rotateByAngle(l, 1, -2, 0).getBlock().getType().toString().contains("SEA"))){
                if (!jumping) {
                    Vector v = new Vector(0.5, 0, 0).rotateAroundY(Math.toRadians(-angleOfRotaion));
                    this.move(EnumMoveType.SELF, new Vec3D(v.getX(), v.getY(), v.getZ()));
                }
                return;
            }

            if (!isBadBlock(rotateByAngle(l, 0, 2, 0)))
                l_ = rotateByAngle(l, 0, 2, 0);
            else if (!isBadBlock(rotateByAngle(l, 1, 0, 0)) )
                l_ = rotateByAngle(l, 1, 0, 0);
            else if (!isBadBlock(rotateByAngle(l, 1, -1, 0))) {
                if (rotateByAngle(l, 1, 0, 0).getBlock().isPassable())
                    l_ = rotateByAngle(l, 1, -1, 0);
                else {
                    Vector v = new Vector(0.3, 0, 0).rotateAroundY(Math.toRadians(-angleOfRotaion));
                    this.move(EnumMoveType.SELF, new Vec3D(v.getX(), v.getY(), v.getZ()));
                    return;
                }
            }
            // If free to move, then do it
            else {
                if (!jumping) {
                    Vector v = new Vector(0.5, 0, 0).rotateAroundY(Math.toRadians(-angleOfRotaion));
                    this.move(EnumMoveType.SELF, new Vec3D(v.getX(), v.getY(), v.getZ()));
                }
                return;
            }

        } else { // On y level
            if (!isBadBlock(rotateByAngle(l, 1, 0, 0)))
                l_ = rotateByAngle(l, 1, 0, 0);
            // If free to move, then do it
            else {
                if (!jumping) {
                    Vector v = new Vector(0.3, 0, 0).rotateAroundY(Math.toRadians(-angleOfRotaion));
                    this.move(EnumMoveType.SELF, new Vec3D(v.getX(), v.getY(), v.getZ()));
                }
                return;
            }
        }

        tryBreak(l_);
    }

    @Override
    public void move(EnumMoveType moveType, Vec3D vec){
        super.move(moveType, vec);
        this.setYawPitch(yaw, 0);
    }

    /**
     * Create an equipment packet from the bot's current equipment
     * @param entity Entity that the packet is about
     * @return The equipment packet
     */
    private PacketPlayOutEntityEquipment getEquipmentPacket(EntityPlayer entity){
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

    /**
     * Determines if a block is not able to be mined
     * @param l The location of the block
     * @return If the block is not minable
     */
    private boolean isBadBlock(Location l){
        org.bukkit.block.Block b = l.getBlock();
        String type = b.getType().toString();

        // If block material matches any of these conditions, allow it:
        if (type.contains("INFESTED") || type.contains("LEAVE") || type.contains("ICE"))
            return false;

        // Return if the block is not solid (not exactly, but close enough)
        return (b.getDrops().isEmpty() || b.isPassable() || b.isLiquid());
    }

    // TODO - document this
    private boolean isUnbreakable(Location l){
        return (isBadBlock(l) && !l.getBlock().isEmpty() && !l.getBlock().isPassable());
    }

    /**
     * Determines how effective a tool is against a block
     *
     * (Full Formula is on the Wiki)
     * @param e The entity mining
     * @param tool The tool in question
     * @param block The block in question
     * @return The strength vs the block given the tool data and entity data
     */
    private double strengthVsBlock(net.minecraft.server.v1_16_R3.Entity e, ItemStack tool, org.bukkit.block.Block block) {
        double hardness = ((CraftBlockData) Bukkit.createBlockData(block.getType())).getState().getBlock().getBlockData().strength;
        if (hardness < 0)
            return 0;

        double mult = 1;

        double effLevel = tool.getEnchantmentLevel(Enchantment.DIG_SPEED);

        if (true) // toolIsEffective(tool, block))
            mult *= 8; // tool effectiveness
        if (effLevel > 0){
            mult += effLevel * effLevel + 1;
        }
        if (e.getBukkitEntity().getLocation().getBlock().getType() == Material.WATER && e.getBukkitEntity().getLocation().add(0,1,0).getBlock().getType() == Material.WATER)
            mult /= 5;
        if (!e.isOnGround())
            mult /= 5;

        return mult / hardness / 30;
    }

    /**
     * Initialize some important values
     */
    static {
        toolMap.put(net.minecraft.server.v1_16_R3.Material.AIR, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.STRUCTURE_VOID, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.PORTAL, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.WOOL, Material.SHEARS);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.PLANT, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.WATER_PLANT, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.REPLACEABLE_PLANT, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.h, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.REPLACEABLE_WATER_PLANT, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.WATER, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.BUBBLE_COLUMN, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.LAVA, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.PACKED_ICE, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.FIRE, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.ORIENTABLE, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.WEB, Material.DIAMOND_SWORD);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.BUILDABLE_GLASS, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.CLAY, Material.DIAMOND_SHOVEL);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.EARTH, Material.DIAMOND_SHOVEL);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.GRASS, Material.DIAMOND_SHOVEL);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.SNOW_LAYER, Material.DIAMOND_SHOVEL);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.SAND, Material.DIAMOND_SHOVEL);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.SPONGE, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.SHULKER_SHELL, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.WOOD, Material.DIAMOND_AXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.NETHER_WOOD, Material.DIAMOND_AXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.BAMBOO_SAPLING, Material.DIAMOND_SWORD);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.BAMBOO, Material.DIAMOND_SWORD);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.CLOTH, Material.SHEARS);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.TNT, null);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.LEAVES, Material.DIAMOND_HOE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.SHATTERABLE, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.ICE, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.CACTUS, Material.DIAMOND_HOE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.STONE, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.ORE, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.SNOW_BLOCK, Material.DIAMOND_SHOVEL);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.HEAVY, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.BANNER, Material.DIAMOND_AXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.PISTON, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.CORAL, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.PUMPKIN, Material.DIAMOND_AXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.DRAGON_EGG, Material.DIAMOND_PICKAXE);
        toolMap.put(net.minecraft.server.v1_16_R3.Material.CAKE, null);
    }
}
