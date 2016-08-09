package com.nomercymc.oldpvp.nms;

import net.minecraft.server.v1_9_R2.NBTTagCompound;
import net.minecraft.server.v1_9_R2.NBTTagDouble;
import net.minecraft.server.v1_9_R2.NBTTagInt;
import net.minecraft.server.v1_9_R2.NBTTagList;
import net.minecraft.server.v1_9_R2.NBTTagString;
import org.bukkit.craftbukkit.v1_9_R2.inventory.CraftItemStack;

import com.nomercymc.oldpvp.DamageValues;

public class DamageValues_1_9_R2
  implements DamageValues
{
  public org.bukkit.inventory.ItemStack changeDamageValue(org.bukkit.inventory.ItemStack i, double damageValue)
  {
    net.minecraft.server.v1_9_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(i);
    NBTTagCompound compound = null;
    if (nmsStack != null)
    {
      if (nmsStack.hasTag()) {
        compound = nmsStack.getTag();
      } else {
        compound = new NBTTagCompound();
      }
      NBTTagList modifiers = new NBTTagList();
      NBTTagCompound damage = new NBTTagCompound();
      damage.set("AttributeName", new NBTTagString("generic.attackDamage"));
      damage.set("Name", new NBTTagString("generic.attackDamage"));
      damage.set("Amount", new NBTTagDouble(damageValue));
      damage.set("Operation", new NBTTagInt(0));
      damage.set("UUIDLeast", new NBTTagInt(894654));
      damage.set("UUIDMost", new NBTTagInt(2872));
      damage.set("Slot", new NBTTagString("mainhand"));
      damage.set("Slot0", new NBTTagString("offhand"));
      modifiers.add(damage);
      compound.set("AttributeModifiers", modifiers);
      nmsStack.setTag(compound);
      return CraftItemStack.asBukkitCopy(nmsStack);
    }
    return null;
  }
}
