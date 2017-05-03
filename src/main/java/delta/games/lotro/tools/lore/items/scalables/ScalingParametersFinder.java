package delta.games.lotro.tools.lore.items.scalables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import delta.games.lotro.character.stats.BasicStatsSet;
import delta.games.lotro.character.stats.STAT;
import delta.games.lotro.lore.items.Armour;
import delta.games.lotro.lore.items.EquipmentLocation;
import delta.games.lotro.lore.items.Item;
import delta.games.lotro.lore.items.ItemPropertyNames;
import delta.games.lotro.lore.items.ItemQuality;
import delta.games.lotro.lore.items.stats.ItemStatSliceData;
import delta.games.lotro.lore.items.stats.SlicesBasedItemStatsProvider;
import delta.games.lotro.utils.FixedDecimalsInteger;

/**
 * Computes scaling parameters from raw stat values.
 * @author DAM
 */
public class ScalingParametersFinder
{
  private Map<Integer,SliceCountFinder> _converters;
  private Map<Integer,ArmourDescriptionFinder> _armourConverters;

  /**
   * Constructor.
   */
  public ScalingParametersFinder()
  {
    _converters=new HashMap<Integer,SliceCountFinder>();
    _armourConverters=new HashMap<Integer,ArmourDescriptionFinder>();
  }

  /**
   * Find scaling parameters for the given items.
   * @param items Items to use.
   */
  public void findScalingParameters(List<Item> items)
  {
    for(Item item : items)
    {
      findScalingParameters(item);
    }
  }

  private void findScalingParameters(Item item)
  {
    BasicStatsSet stats=item.getStats();
    if (stats.getStatsCount()==0)
    {
      System.err.println("Cannot manage item: " + item + ": no stats");
      return;
    }
    Integer itemLevel=item.getItemLevel();
    if (itemLevel==null)
    {
      System.err.println("Cannot manage item: " + item + ": no item level");
      return;
    }
    SliceCountFinder converter=_converters.get(itemLevel);
    if (converter==null)
    {
      converter=new SliceCountFinder(itemLevel.intValue());
      _converters.put(itemLevel,converter);
    }
    if (item instanceof Armour)
    {
      int armourValue=((Armour)item).getArmourValue();
      stats.setStat(STAT.ARMOUR,new FixedDecimalsInteger(armourValue));
    }

    SlicesBasedItemStatsProvider provider=new SlicesBasedItemStatsProvider();
    for(STAT stat : stats.getStats())
    {
      FixedDecimalsInteger value=stats.getStat(stat);
      int statValue=value.getInternalValue();
      ItemStatSliceData slice=null;
      // Specific case for armour
      if (stat==STAT.ARMOUR)
      {
        ArmourDescriptionFinder armourConverter=_armourConverters.get(itemLevel);
        if (armourConverter==null)
        {
          armourConverter=new ArmourDescriptionFinder(itemLevel.intValue());
          _armourConverters.put(itemLevel,armourConverter);
        }
        EquipmentLocation location=item.getEquipmentLocation();
        ItemQuality quality=item.getQuality();
        String label=armourConverter.getArmourDescription(statValue,location,quality);
        if (label!=null)
        {
          slice=new ItemStatSliceData(stat,null,label);
        }
        else
        {
          System.err.println("Armour not found: "+statValue+" for item "+item);
        }
      }
      // Other stats
      else
      {
        Float sliceCount=converter.getSliceCount(stat,statValue);
        if (sliceCount!=null)
        {
          slice=new ItemStatSliceData(stat,sliceCount,null);
        }
        else
        {
          System.err.println("Stat not found: "+stat+"="+statValue+" for item "+item);
        }
      }
      if (slice!=null)
      {
        provider.addSlice(slice);
      }
      else
      {
        provider.setStat(stat,value);
      }
    }
    stats.removeStat(STAT.ARMOUR);
    String slices=provider.toPersistableString();
    item.setProperty(ItemPropertyNames.SLICED_STATS,slices);
    item.setProperty("itemLevels","[176-217]");
  }
}