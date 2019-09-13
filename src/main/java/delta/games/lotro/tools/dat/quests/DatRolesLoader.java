package delta.games.lotro.tools.dat.quests;

import delta.games.lotro.dat.data.DataFacade;
import delta.games.lotro.dat.data.PropertiesSet;
import delta.games.lotro.dat.data.enums.EnumMapper;
import delta.games.lotro.lore.npc.NpcDescription;
import delta.games.lotro.lore.quests.QuestDescription;
import delta.games.lotro.lore.quests.dialogs.DialogElement;
import delta.games.lotro.tools.dat.utils.DatUtils;
import delta.games.lotro.tools.dat.utils.NpcLoader;
import delta.games.lotro.utils.Proxy;

/**
 * Loader for roles data from DAT files.
 * @author DAM
 */
public class DatRolesLoader
{
  private DataFacade _facade;
  private EnumMapper _questRoleAction;

  /**
   * Constructor.
   * @param facade Data facade.
   */
  public DatRolesLoader(DataFacade facade)
  {
    _facade=facade;
    _questRoleAction=facade.getEnumsManager().getEnumMapper(587202589);
  }

  /**
   * Load roles.
   * @param quest Quest.
   * @param properties Input properties.
   */
  public void loadRoles(QuestDescription quest, PropertiesSet properties)
  {
    //handleGlobalRoles(quest,properties);
    handleRoles(quest,properties);
  }

  void handleGlobalRoles(QuestDescription quest, PropertiesSet properties)
  {
    // Quest_GlobalRoles
    Object[] roles=(Object[])properties.getProperty("Quest_GlobalRoles");
    if (roles!=null)
    {
      System.out.println("Roles (global):");
      int index=0;
      for(Object roleObj : roles)
      {
        System.out.println("Index: "+index);
        PropertiesSet roleProps=(PropertiesSet)roleObj;
        Integer dispenserAction=(Integer)roleProps.getProperty("QuestDispenser_Action");
        if (dispenserAction!=null)
        {
          String action=_questRoleAction.getString(dispenserAction.intValue());
          System.out.println("\tdispenserAction: " +action); // LeaveInstance
        }
        Integer npcId=(Integer)roleProps.getProperty("QuestDispenser_NPC");
        if (npcId!=null)
        {
          String npcName=NpcLoader.loadNPC(_facade,npcId.intValue());
          System.out.println("\tNPC: "+npcName);
        }
        String dispenserRoleName=(String)roleProps.getProperty("QuestDispenser_RoleName");
        if (dispenserRoleName!=null)
        {
          System.out.println("\tdispenserRolename: " +dispenserRoleName);
        }
        String dispenserRoleConstraint=(String)roleProps.getProperty("QuestDispenser_RoleConstraint");
        if (dispenserRoleConstraint!=null)
        {
          System.out.println("\tdispenserRole Constraint: " +dispenserRoleConstraint);
        }
        String successText=DatUtils.getFullStringProperty(roleProps,"QuestDispenser_RoleSuccessText","{***}");
        System.out.println("\tSuccess text: "+successText);
        index++;
      }
    }
  }

  void handleRoles(QuestDescription quest, PropertiesSet properties)
  {
    // Quest_RoleArray
    Object[] roles=(Object[])properties.getProperty("Quest_RoleArray");
    if (roles!=null)
    {
      System.out.println("Role array:");
      int index=0;
      for(Object roleObj : roles)
      {
        PropertiesSet roleProps=(PropertiesSet)roleObj;
        System.out.println("Index: "+index);
        int dispenserAction=((Integer)roleProps.getProperty("QuestDispenser_Action")).intValue();
        String action=_questRoleAction.getString(dispenserAction);
        System.out.println("\tdispenserAction: " +action);
        Integer objectiveIndex=(Integer)roleProps.getProperty("Quest_ObjectiveIndex");
        System.out.println("\tobjectiveIndex: " +objectiveIndex);
        String dispenserRoleName=(String)roleProps.getProperty("QuestDispenser_RoleName");
        if (dispenserRoleName!=null)
        {
          System.out.println("\tdispenserRolename: " +dispenserRoleName);
        }
        Integer npcId=(Integer)roleProps.getProperty("QuestDispenser_NPC");
        if (npcId!=null)
        {
          String npcName=NpcLoader.loadNPC(_facade,npcId.intValue());
          System.out.println("\tNPC: "+npcName);
        }
        String successText=DatUtils.getFullStringProperty(roleProps,"QuestDispenser_RoleSuccessText",Markers.CHARACTER);
        System.out.println("\tSuccess text: "+successText);
        String failureText=DatUtils.getFullStringProperty(roleProps,"QuestDispenser_RoleFailureText",Markers.CHARACTER);
        if (failureText!=null)
        {
          System.out.println("\tFailure text: "+failureText);
        }
        if ((objectiveIndex!=null) && (objectiveIndex.intValue()==0) && (dispenserAction==6)) // Bestow
        {
          addBestower(quest,npcId,successText);
        }
        index++;
      }
    }
  }

  private void addBestower(QuestDescription quest, Integer npcId, String successText)
  {
    DialogElement bestower=new DialogElement();
    if (npcId!=null)
    {
      String npcName=NpcLoader.loadNPC(_facade,npcId.intValue());
      Proxy<NpcDescription> npc=new Proxy<NpcDescription>();
      npc.setId(npcId.intValue());
      npc.setName(npcName);
      bestower.setWho(npc);
    }
    bestower.setWhat(successText);
    quest.addBestower(bestower);
  }
}
