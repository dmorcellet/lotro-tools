package delta.games.lotro.tools.dat.maps.landblocks;

import java.io.ByteArrayInputStream;

import org.apache.log4j.Logger;

import delta.games.lotro.dat.DATFilesConstants;
import delta.games.lotro.dat.archive.DATArchive;
import delta.games.lotro.dat.archive.DatFilesManager;
import delta.games.lotro.dat.data.DataFacade;
import delta.games.lotro.dat.data.PropertiesSet;
import delta.games.lotro.dat.data.PropertiesSet.PropertyValue;
import delta.games.lotro.dat.utils.BufferUtils;
import delta.games.lotro.tools.dat.maps.data.PropertiesDescriptor;

/**
 * Loader for block maps.
 * @author DAM
 */
public class BlockMapLoader
{
  private static final Logger LOGGER=Logger.getLogger(BlockMapLoader.class);

  private DataFacade _facade;
  private PropertyDescriptorsManager _descriptorsMgr;

  /**
   * Constructor.
   * @param facade Data facade.
   */
  public BlockMapLoader(DataFacade facade)
  {
    _facade=facade;
    _descriptorsMgr=new PropertyDescriptorsManager(facade);
  }

  /**
   * Load the properties for a map block.
   * @param region Region identifier.
   * @param blockX Block coordinate (horizontal).
   * @param blockY Block coordinate (vertical).
   * @return the loaded properties or <code>null</code> if no such block.
   */
  public PropertiesSet loadPropertiesForMapBlock(int region, int blockX, int blockY)
  {
    long blockMapDID=0x80100000L+(region*0x10000)+(blockX*0x100)+blockY;
    return loadPropertiesForMapBlock(blockMapDID);
  }

  /**
   * Load the properties for a map block.
   * @param blockMapDID Map block identifier.
   * @return the loaded properties or <code>null</code> if no such block.
   */
  public PropertiesSet loadPropertiesForMapBlock(long blockMapDID)
  {
    DatFilesManager datFilesMgr=_facade.getDatFilesManager();
    int region=(int)((blockMapDID&0xF0000)>>16);
    DATArchive map=datFilesMgr.getArchive(DATFilesConstants.MAP_SEED+region);
    byte[] data=map.loadEntry(blockMapDID);
    if (data==null)
    {
      return null;
    }
    PropertiesDescriptor descriptor=_descriptorsMgr.getDescriptorForRegion(region);
    if (descriptor==null)
    {
      return null;
    }
    ByteArrayInputStream bis=new ByteArrayInputStream(data);
    long did=BufferUtils.readUInt32AsLong(bis);
    if (did!=blockMapDID)
    {
      throw new IllegalArgumentException("Expected DID for block map: "+blockMapDID);
    }
    PropertiesSet props=new PropertiesSet();
    int count=BufferUtils.readTSize(bis);
    //System.out.println(count+" entries in this block map!");
    for(int i=0;i<count;i++)
    {
      int key=BufferUtils.readUInt32(bis);
      int index=BufferUtils.readUInt16(bis);
      PropertyValue value=descriptor.getPropertyValue(key,index);
      if (value!=null)
      {
        props.setProperty(value);
      }
      //System.out.println("Mapping key="+key+" to index="+index);
    }
    Integer areaDID=(Integer)props.getProperty("Area_DID");
    if (areaDID==null)
    {
      LOGGER.warn("No area DID for land block: "+blockMapDID);
    }
    int available=bis.available();
    if (available>0)
    {
      LOGGER.warn("Available bytes: "+available);
    }
    return props;
  }
}
