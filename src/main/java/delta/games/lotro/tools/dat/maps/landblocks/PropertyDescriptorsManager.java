package delta.games.lotro.tools.dat.maps.landblocks;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import delta.games.lotro.dat.data.DataFacade;
import delta.games.lotro.dat.data.PropertiesSet.PropertyValue;
import delta.games.lotro.dat.data.PropertyDefinition;
import delta.games.lotro.dat.loaders.DBPropertiesLoader;
import delta.games.lotro.dat.utils.BufferUtils;
import delta.games.lotro.tools.dat.maps.data.PropertiesDescriptor;

/**
 * Manager for property desc(riptors?).
 * @author DAM
 */
public class PropertyDescriptorsManager
{
  private static final Logger LOGGER=Logger.getLogger(PropertyDescriptorsManager.class);

  private static final int[] PROPERTY_DESC_DIDS= { 0x18000000, 0x18000014, 0x18000015, 0x1800001a};

  private DataFacade _facade;
  private List<PropertiesDescriptor> _descriptors;

  /**
   * Constructor.
   * @param facade Data facade.
   */
  public PropertyDescriptorsManager(DataFacade facade)
  {
    _facade=facade;
    _descriptors=new ArrayList<PropertiesDescriptor>();
    init();
  }

  private void loadPropertyDescEntr(ByteArrayInputStream bis, PropertiesDescriptor storage)
  {
    //System.out.println("****** Property descriptor entry:");
    // Property ID
    int propertyID=BufferUtils.readUInt32(bis);
    int echo=BufferUtils.readUInt32(bis);
    if (echo!=propertyID)
    {
      throw new IllegalArgumentException("Mismatch property ID: "+propertyID);
    }
    PropertyDefinition propertyDef=_facade.getPropertiesRegistry().getPropertyDef(propertyID);
    if (propertyDef==null)
    {
      throw new IllegalStateException("Property definition not found: ID="+propertyID);
    }
    //System.out.println(propertyDef);
    // Block map key
    int blockMapKey=BufferUtils.readUInt32(bis);
    //System.out.println("Block map key: "+blockMapKey);
    /*int unknown=*/BufferUtils.readUInt8(bis); // 0 or 1
    int count=BufferUtils.readUInt32(bis);
    //System.out.println(count+" properties to load!");
    DBPropertiesLoader propsLoader=new DBPropertiesLoader(_facade);
    for(int i=0;i<count;i++)
    {
      PropertyValue propertyValue=propsLoader.decodeProperty(bis,false);
      //System.out.println(propertyValue);
      storage.addPropertyValue(blockMapKey,propertyValue);
    }
  }

  /**
   * Load some property descriptors.
   * @param propertyDescId Property descriptor ID.
   * @return the loaded data.
   */
  public PropertiesDescriptor loadPropertiesDescriptor(int propertyDescId)
  {
    //System.out.println("************** Property descriptor: "+propertyDescId);
    byte[] data=_facade.loadData(propertyDescId);
    ByteArrayInputStream bis=new ByteArrayInputStream(data);
    int did=BufferUtils.readUInt32(bis);
    if (did!=propertyDescId)
    {
      throw new IllegalArgumentException("Expected DID for property desc: "+propertyDescId);
    }
    int zero=BufferUtils.readUInt32(bis);
    if (zero!=0)
    {
      throw new IllegalArgumentException("Expected 0 here. Found: "+zero);
    }
    PropertiesDescriptor ret=new PropertiesDescriptor();
    int count=BufferUtils.readTSize(bis);
    //System.out.println(count+" property descriptor entries to load!");
    for(int i=0;i<count;i++)
    {
      loadPropertyDescEntr(bis,ret);
    }
    int available=bis.available();
    if (available>0)
    {
      LOGGER.warn("Available bytes: "+available);
    }
    return ret;
  }

  private void init()
  {
    for(int propertyDescId : PROPERTY_DESC_DIDS)
    {
      PropertiesDescriptor descriptors=loadPropertiesDescriptor(propertyDescId);
      _descriptors.add(descriptors);
    }
  }

  /**
   * Get the properties descriptor for a region.
   * @param region Region to use.
   * @return A properties descriptor or <code>null</code> if not found.
   */
  public PropertiesDescriptor getDescriptorForRegion(int region)
  {
    if ((region>=1) && (region<=_descriptors.size()))
    {
      return _descriptors.get(region-1);
    }
    return null;
  }
}