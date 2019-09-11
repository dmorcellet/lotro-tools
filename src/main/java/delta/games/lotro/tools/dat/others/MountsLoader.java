package delta.games.lotro.tools.dat.others;

import java.io.File;
import java.util.BitSet;

import org.apache.log4j.Logger;

import delta.games.lotro.dat.DATConstants;
import delta.games.lotro.dat.data.DataFacade;
import delta.games.lotro.dat.data.PropertiesSet;
import delta.games.lotro.dat.data.enums.EnumMapper;
import delta.games.lotro.dat.utils.BitSetUtils;
import delta.games.lotro.dat.utils.DatIconsUtils;
import delta.games.lotro.tools.dat.utils.DatUtils;

/**
 * Get definition of mounts from DAT files.
 * @author DAM
 */
public class MountsLoader
{
  private static final Logger LOGGER=Logger.getLogger(MountsLoader.class);

  private DataFacade _facade;
  private EnumMapper _mountType;
  private EnumMapper _category;
  private EnumMapper _subCategory;

  /**
   * Constructor.
   * @param facade Data facade.
   */
  public MountsLoader(DataFacade facade)
  {
    _facade=facade;
    _mountType=facade.getEnumsManager().getEnumMapper(587203200);
    _category=facade.getEnumsManager().getEnumMapper(587202586);
    _subCategory=facade.getEnumsManager().getEnumMapper(587203478);
  }

  /**
   * Load a mount definition.
   * @param indexDataId Mount skill identifier.
   */
  public void load(int indexDataId)
  {
    PropertiesSet properties=_facade.loadProperties(indexDataId+DATConstants.DBPROPERTIES_OFFSET);
    if (properties!=null)
    {
      System.out.println("************* "+indexDataId+" *************");
      //System.out.println(properties.dump());

      // Hidden?
      int hidden=((Integer)properties.getProperty("Collection_Hide_Entry")).intValue();
      System.out.println("Hidden: "+hidden);
      // Name
      String name=DatUtils.getStringProperty(properties,"Skill_Name");
      System.out.println("Name: "+name);
      // Description
      String description=DatUtils.getStringProperty(properties,"Skill_Desc");
      System.out.println("Description: "+description);
      // Icon
      int iconId=((Integer)properties.getProperty("Skill_Icon")).intValue();
      int largeIconId=((Integer)properties.getProperty("Skill_LargeIcon")).intValue();
      int smallIconId=((Integer)properties.getProperty("Skill_SmallIcon")).intValue();
      System.out.println("Icons: "+smallIconId+" / "+iconId+" / "+largeIconId);
      if ((iconId!=smallIconId) || (iconId!=largeIconId))
      {
        LOGGER.warn("Icons mismatch: small="+smallIconId+"/regular="+iconId+"/large="+largeIconId);
      }
      File collectionsDir=new File("collections");
      File cosmeticPetsDir=new File(collectionsDir,"mounts");
      File to=new File(cosmeticPetsDir,iconId+".png");
      DatIconsUtils.buildImageFile(_facade,iconId,to);
      // Source description (null for war-steeds)
      String sourceDescription=DatUtils.getStringProperty(properties,"Collection_Piece_SourceDesc");
      System.out.println("Source description: "+sourceDescription);

      // Category
      int categoryCode=((Integer)properties.getProperty("Skill_Category")).intValue();
      String category=_category.getString(categoryCode);
      System.out.println("Category: "+category);
      // Sub category
      int subCategoryCode=((Integer)properties.getProperty("Skill_SubCategory")).intValue();
      String subCategory=_subCategory.getString(subCategoryCode);
      System.out.println("Sub category: "+subCategory);

      Object[] effectsList=(Object[])properties.getProperty("Skill_Toggle_Effect_List");
      if (effectsList!=null)
      {
        for(Object effectObj : effectsList)
        {
          PropertiesSet effectRefProps=(PropertiesSet)effectObj;
          int effectId=((Integer)effectRefProps.getProperty("Skill_Toggle_Effect")).intValue();
          PropertiesSet effectProps=_facade.loadProperties(effectId+DATConstants.DBPROPERTIES_OFFSET);
          //System.out.println(effectProps.dump());

          // Initial Name
          String initialName=DatUtils.getStringProperty(effectProps,"Mount_Name_Initial");
          if (initialName==null) continue;
          System.out.println("Initial name: "+initialName);
          // Mount type
          int mountTypeCode=((Integer)effectProps.getProperty("Mount_Type")).intValue();
          BitSet mountTypesBitSet=BitSetUtils.getBitSetFromFlags(mountTypeCode);
          String mountTypes=BitSetUtils.getStringFromBitSet(mountTypesBitSet,_mountType,"/");
          System.out.println("Mount type(s): "+mountTypes);
          // Morale
          int morale=((Integer)effectProps.getProperty("Mount_Durability_Base_Max")).intValue();
          System.out.println("Morale: "+morale);
          // Speed
          Float speed=null;
          Object[] mods=(Object[])effectProps.getProperty("Mod_Array");
          for(Object modObj : mods)
          {
            PropertiesSet modProps=(PropertiesSet)modObj;
            speed=(Float)modProps.getProperty("ForwardSource_Movement_MountSpeedMultiplier");
            if (speed!=null)
            {
              break;
            }
          }
          System.out.println("Speed: "+speed); // Always present
        }
      }
    }
    else
    {
      LOGGER.warn("Could not handle mount skill ID="+indexDataId);
    }
  }

  private void doIt()
  {
    PropertiesSet mountsDirectoryProps=_facade.loadProperties(0x70048B29+DATConstants.DBPROPERTIES_OFFSET);
    Object[] mountSkillsList=(Object[])mountsDirectoryProps.getProperty("Mount_SkillList");
    for(Object mountSkillObj : mountSkillsList)
    {
      int mountSkillId=((Integer)mountSkillObj).intValue();
      load(mountSkillId);
    }
  }

  /**
   * Main method for this tool.
   * @param args Not used.
   */
  public static void main(String[] args)
  {
    DataFacade facade=new DataFacade();
    new MountsLoader(facade).doIt();
    facade.dispose();
  }
}
