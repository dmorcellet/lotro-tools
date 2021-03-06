package delta.games.lotro.tools.lore.items;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import delta.common.utils.files.TextFileWriter;
import delta.common.utils.text.TextUtils;
import delta.games.lotro.LotroCoreConfig;
import delta.games.lotro.lore.items.Item;
import delta.games.lotro.lore.items.ItemsManager;
import delta.games.lotro.lore.recipes.Recipe.ItemReference;
import delta.games.lotro.utils.Escapes;
import delta.games.lotro.utils.LotroIconsManager;
import delta.games.lotro.utils.LotroLoggers;

/**
 * Manager for items and icons loading.
 * @author DAM
 */
public class ItemsAndIconsManager
{
  private static final Logger LOGGER=LotroLoggers.getLotroLogger();

  private HashSet<String> _handledIcons=new HashSet<String>();
  private HashSet<String> _handledItems=new HashSet<String>();

  private File _workDir;
  
  /**
   * Constructor.
   * @param workDir Workspace directory.
   */
  public ItemsAndIconsManager(File workDir)
  {
    _workDir=workDir;
  }

  /**
   * Load maps from files.
   */
  public void loadMaps()
  {
    File iconFile=new File(_workDir,"icons.txt");
    List<String> lines=TextUtils.readAsLines(iconFile);
    if (lines!=null)
    {
      _handledIcons.addAll(lines);
    }
    File itemsFile=new File(_workDir,"items.txt");
    lines=TextUtils.readAsLines(itemsFile);
    if (lines!=null)
    {
      _handledItems.addAll(lines);
    }
  }

  /**
   * Save maps to files.
   */
  public void saveMaps()
  {
    File iconFile=new File(_workDir,"icons.txt");
    saveMap(_handledIcons,iconFile);
    File itemsFile=new File(_workDir,"items.txt");
    saveMap(_handledItems,itemsFile);
  }

  private void saveMap(HashSet<String> map, File toFile)
  {
    TextFileWriter w=new TextFileWriter(toFile);
    if (w.start())
    {
      for(String l : map)
      {
        w.writeNextLine(l);
      }
      w.terminate();
    }
  }

  /**
   * Handle an icon.
   * @param iconURL URL of the icon.
   */
  public void handleIcon(String iconURL)
  {
    if (!_handledIcons.contains(iconURL))
    {
      try
      {
        LotroIconsManager iconsManager=LotroIconsManager.getInstance();
        File f=iconsManager.getIconFile(iconURL);
        if (f!=null)
        {
          iconURL=new String(iconURL);
          _handledIcons.add(iconURL);
        }
        System.out.println("Icon: "+f);
      }
      catch(Throwable t)
      {
        LOGGER.error("Caught error", t);
      }
    }
  }

  private void loadItems(String key)
  {
    System.out.println("Loading item ["+key+"]");
    File itemsDir=LotroCoreConfig.getInstance().getItemsDir();
    if (!itemsDir.exists())
    {
      itemsDir.mkdirs();
    }
    key=Escapes.escapeIdentifier(key);
    String url="http://lorebook.lotro.com/wiki/"+key;
    ItemPageParser parser=new ItemPageParser();
    List<Item> items=parser.parseItemPage(url);
    if ((items!=null) && (items.size()>0))
    {
      ItemsManager itemsManager=ItemsManager.getInstance();
      for(Item item : items)
      {
        System.out.println(item.dump());
        int id=item.getIdentifier();
        if (id!=0)
        {
          itemsManager.writeItemFile(item);
        }
        else
        {
          LOGGER.warn("Item ["+key+"]: identifier=0!");
        }
      }
    }
    else
    {
      LOGGER.error("Cannot parse item ["+key+"] at URL ["+url+"]!");
    }
  }

  /**
   * Handle an item.
   * @param itemKey Item key.
   */
  public void handleItemKey(String itemKey)
  {
    if (itemKey!=null)
    {
      if (!_handledItems.contains(itemKey))
      {
        System.out.println("Item: "+itemKey);
        try
        {
          loadItems(itemKey);
          itemKey=new String(itemKey);
          _handledItems.add(itemKey);
        }
        catch(Throwable t)
        {
          System.out.println("Error:");
          LOGGER.error("Caught error", t);
        }
      }
    }
  }

  /**
   * Handle an item.
   * @param item Item reference.
   */
  public void handleItem(ItemReference item)
  {
    if (item!=null)
    {
      String iconURL=item.getIcon();
      if (iconURL!=null)
      {
        handleIcon(iconURL);
      }
      String itemKey=item.getItemKey();
      handleItemKey(itemKey);
    }
  }
}
