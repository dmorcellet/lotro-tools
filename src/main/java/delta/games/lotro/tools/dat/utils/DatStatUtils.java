package delta.games.lotro.tools.dat.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import delta.games.lotro.common.progression.ProgressionsManager;
import delta.games.lotro.common.stats.ConstantStatProvider;
import delta.games.lotro.common.stats.RangedStatProvider;
import delta.games.lotro.common.stats.ScalableStatProvider;
import delta.games.lotro.common.stats.StatDescription;
import delta.games.lotro.common.stats.StatOperator;
import delta.games.lotro.common.stats.StatProvider;
import delta.games.lotro.common.stats.StatUtils;
import delta.games.lotro.common.stats.StatsProvider;
import delta.games.lotro.common.stats.StatsRegistry;
import delta.games.lotro.common.stats.TieredScalableStatProvider;
import delta.games.lotro.dat.DATConstants;
import delta.games.lotro.dat.data.DataFacade;
import delta.games.lotro.dat.data.PropertiesSet;
import delta.games.lotro.dat.data.PropertyDefinition;
import delta.games.lotro.utils.maths.Progression;

/**
 * Utility methods related to stats from DAT files.
 * @author DAM
 */
public class DatStatUtils
{
  private static final Logger LOGGER=Logger.getLogger(DatStatUtils.class);

  /**
   * Flag to indicate if stats shall be filtered or not.
   */
  public static boolean doFilterStats=true;

  /**
   * Progressions manager.
   */
  public static ProgressionsManager _progressions=new ProgressionsManager();

  /**
   * Stats usage statistics.
   */
  public static StatsUsageStatistics _statsUsageStatistics=new StatsUsageStatistics();

  /**
   * Load a set of stats from some properties.
   * @param facade Data facade.
   * @param properties Properties to use to get stats.
   * @return A stats provider.
   */
  public static StatsProvider buildStatProviders(DataFacade facade, PropertiesSet properties)
  {
    return buildStatProviders(null,facade,properties);
  }

  /**
   * Load a set of stats from some properties.
   * @param propsPrefix Prefix for properties to use, default is <code>null</code>.
   * @param facade Data facade.
   * @param properties Properties to use to get stats.
   * @return A stats provider.
   */
  public static StatsProvider buildStatProviders(String propsPrefix, DataFacade facade, PropertiesSet properties)
  {
    String arrayPropName="Mod_Array";
    String modifiedPropName="Mod_Modified";
    String progressionPropName="Mod_Progression";
    if (propsPrefix!=null)
    {
      arrayPropName=propsPrefix+arrayPropName;
      modifiedPropName=propsPrefix+modifiedPropName;
      progressionPropName=propsPrefix+progressionPropName;
    }

    StatsProvider statsProvider=new StatsProvider();
    Object[] mods=(Object[])properties.getProperty(arrayPropName);
    if (mods!=null)
    {
      Map<StatDescription,RangedStatProvider> rangedStatProviders=null;
      for(int i=0;i<mods.length;i++)
      {
        PropertiesSet statProperties=(PropertiesSet)mods[i];
        Integer statId=(Integer)statProperties.getProperty(modifiedPropName);
        PropertyDefinition def=facade.getPropertiesRegistry().getPropertyDef(statId.intValue());
        StatDescription stat=getStatDescription(def);
        if (stat==null)
        {
          continue;
        }
        //String statKey=def.getName();
        //boolean useStat=useStat(statKey);
        boolean useStat=useStat(stat);
        if (!useStat) continue;
        /*
        String descriptionOverride=getDescriptionOverride(statProperties);
        if (descriptionOverride!=null)
        {
          System.out.println("Description override: ["+descriptionOverride+"] for "+stat.getName()+"="+stat.getKey());
        }
        */
        _statsUsageStatistics.registerStatUsage(stat);
        StatProvider provider=null;
        Number value=null;
        // Often 7 for "add"
        Integer modOp=(Integer)statProperties.getProperty("Mod_Op");
        StatOperator operator=getOperator(modOp);
        Integer progressId=(Integer)statProperties.getProperty(progressionPropName);
        if (progressId!=null)
        {
          provider=buildStatProvider(facade,stat,progressId.intValue());
        }
        else
        {
          Object propValue=statProperties.getProperty(def.getName());
          if (propValue instanceof Number)
          {
            value=(Number)propValue;
            float statValue=StatUtils.fixStatValue(stat,value.floatValue());
            if (Math.abs(statValue)>0.001)
            {
              provider=new ConstantStatProvider(stat,statValue);
            }
          }
          else
          {
            LOGGER.warn("No progression ID and no direct value... Stat is "+stat.getName());
          }
        }
        if (provider!=null)
        {
          Integer minLevel=(Integer)statProperties.getProperty("Mod_ProgressionFloor");
          Integer maxLevel=(Integer)statProperties.getProperty("Mod_ProgressionCeiling");
          if ((minLevel!=null) || (maxLevel!=null))
          {
            RangedStatProvider rangedProvider=null;
            if (rangedStatProviders==null) rangedStatProviders=new HashMap<StatDescription,RangedStatProvider>();
            rangedProvider=rangedStatProviders.get(stat);
            if (rangedProvider==null)
            {
              rangedProvider=new RangedStatProvider(stat);
              rangedStatProviders.put(stat,rangedProvider);
              statsProvider.addStatProvider(rangedProvider);
            }
            rangedProvider.addRange(minLevel,maxLevel,provider);
          }
          else
          {
            statsProvider.addStatProvider(provider);
          }
          provider.setOperator(operator);
        }
      }
    }
    return statsProvider;
  }

  static String getDescriptionOverride(PropertiesSet statProperties)
  {
    String[] descriptionOverride=(String[])statProperties.getProperty("Mod_DescriptionOverride");
    if (descriptionOverride!=null)
    {
      return descriptionOverride.length+": "+Arrays.toString(descriptionOverride);
      /*
      if ((descriptionOverride.length==2) && ("+".equals(descriptionOverride[0].trim())))
      {
        return descriptionOverride[1].trim();
      }
      else if (descriptionOverride.length==1)
      {
        return descriptionOverride[0].trim();
      }
      else
      {
        System.out.println(Arrays.toString(descriptionOverride));
      }
      */
    }
    return null;
  }

  private static StatOperator getOperator(Integer modOpInteger)
  {
    if (modOpInteger==null) return StatOperator.ADD;
    int modOp=modOpInteger.intValue();
    if (modOp==5) return StatOperator.SET;
    if (modOp==6) return StatOperator.SUBSTRACT;
    if (modOp==7) return StatOperator.ADD;
    if (modOp==8) return StatOperator.MULTIPLY;
    LOGGER.warn("Unmanaged operator: "+modOp);
    return null;
  }

  /**
   * Get a progression curve.
   * @param facade Data facade.
   * @param progressId Progression ID.
   * @return A progression curve or <code>null</code> if not found.
   */
  public static Progression getProgression(DataFacade facade, int progressId)
  {
    Progression ret=_progressions.getProgression(progressId);
    if (ret==null)
    {
      int progressPropertiesId=progressId+DATConstants.DBPROPERTIES_OFFSET;
      PropertiesSet progressProperties=facade.loadProperties(progressPropertiesId);
      if (progressProperties!=null)
      {
        ret=ProgressionFactory.buildProgression(progressId, progressProperties);
        if (ret!=null)
        {
          _progressions.registerProgression(progressId,ret);
        }
      }
    }
    return ret;
  }

  /**
   * Build a stat provider from the given progression identifier.
   * @param facade Data facade.
   * @param stat Targeted stat.
   * @param progressId Progression ID.
   * @return A stat provider.
   */
  public static StatProvider buildStatProvider(DataFacade facade, StatDescription stat, int progressId)
  {
    if (progressId==0) return null;
    PropertiesSet properties=facade.loadProperties(progressId+0x9000000);
    Object[] progressionIds=(Object[])properties.getProperty("DataIDProgression_Array");
    if (progressionIds!=null)
    {
      return getTieredProgression(facade,stat,properties);
    }
    Progression progression=getProgression(facade,progressId);
    ScalableStatProvider scalableStat=new ScalableStatProvider(stat,progression);
    return scalableStat;
  }

  /**
   * Get a progression curve.
   * @param facade Data facade.
   * @param stat Involved stat.
   * @param properties Progression properties.
   * @return A progression curve or <code>null</code> if not found.
   */
  private static TieredScalableStatProvider getTieredProgression(DataFacade facade, StatDescription stat, PropertiesSet properties)
  {
    Object[] progressionIds=(Object[])properties.getProperty("DataIDProgression_Array");
    int nbTiers=progressionIds.length;
    TieredScalableStatProvider ret=new TieredScalableStatProvider(stat,nbTiers);
    int tier=1;
    for(Object progressionIdObj : progressionIds)
    {
      int progressionId=((Integer)progressionIdObj).intValue();
      Progression progression=getProgression(facade,progressionId);
      ret.setProgression(tier,progression);
      tier++;
    }
    return ret;
  }

  /**
   * Get a stat description.
   * @param propertyDefinition Property definition.
   * @return A stat description.
   */
  private static StatDescription getStatDescription(PropertyDefinition propertyDefinition)
  {
    StatDescription ret=null;
    StatsRegistry registry=StatsRegistry.getInstance();

    String statKey=propertyDefinition.getName();
    String fixedStatKey=fixStat(statKey);
    if (fixedStatKey!=null)
    {
      ret=registry.getByKey(fixedStatKey);
    }
    else
    {
      int id=propertyDefinition.getPropertyId();
      if (id==0)
      {
        return null;
      }
      ret=registry.getById(id);
    }
    if (ret==null)
    {
      LOGGER.warn("Stat not found: "+propertyDefinition.getName());
    }
    return ret;
  }

  private static boolean useStat(StatDescription stat)
  {
    if (doFilterStats)
    {
      Integer index=stat.getIndex();
      return index!=null;
    }
    return true;
  }

  /*
  private static boolean old_useStat(String key)
  {
    if ("AI_PetEffect_HeraldBaseWC_Override".equals(key)) return false;
    if ("AI_PetEffect_ArcherBaseWC_Override".equals(key)) return false;
    if ("AI_PetEffect_HeraldHopeWC_Override".equals(key)) return false;
    if ("AI_PetEffect_HeraldVictoryWC_Override".equals(key)) return false;
    if ("Trait_Loremaster_PetModStat_Slot2".equals(key)) return false;
    if ("Trait_Captain_PetModStat_Slot4".equals(key)) return false;
    // Only on a test item
    if ("Skill_RiftSet_Absorb_Fire".equals(key)) return false;
    // Gives affinity for RK stones: fire, frost or lightning
    if ("ForwardSource_Combat_TraitCombo".equals(key)) return false;
    if ("Item_Runekeeper_PreludeofHope_Cleanse".equals(key)) return false;
    if ("Skill_EffectOverride_Burglar_ExploitOpening".equals(key)) return false;
    if ("Combat_MeleeDmgQualifier_WeaponProcEffect".equals(key)) return false;
    if ("Item_Minstrel_Oathbreaker_Damagetype".equals(key)) return false;

    if ("Trait_PvMP_BattleRank".equals(key)) return false;
    if ("Skill_VitalCost_Champion_AOEMod".equals(key)) return false;
    if ("Skill_VitalCost_Champion_StrikeMod".equals(key)) return false;
    if ("Skill_InductionDuration_ResearchingMod".equals(key)) return false;
    if ("TotalThreatModifier_Player".equals(key)) return false;
    if ("Skill_InductionDuration_AllSkillsMod".equals(key)) return false;

    return true;
  }
  */

  private static String fixStat(String key)
  {
    if ("Combat_Agent_Armor_Value_Float".equals(key)) return "ARMOUR";
    if ("Combat_MitigationPercentage_Common".equals(key)) return "PHYSICAL_MITIGATION_PERCENTAGE";
    if ("Combat_Class_PhysicalMastery_Unified".equals(key)) return "PHYSICAL_MASTERY";
    if ("Combat_Class_TacticalMastery_Unified".equals(key)) return "TACTICAL_MASTERY";
    return null;
  }
}
