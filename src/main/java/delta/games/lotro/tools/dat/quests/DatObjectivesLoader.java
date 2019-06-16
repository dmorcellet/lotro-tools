package delta.games.lotro.tools.dat.quests;

import org.apache.log4j.Logger;

import delta.games.lotro.character.skills.SkillDescription;
import delta.games.lotro.dat.data.DataFacade;
import delta.games.lotro.dat.data.PropertiesSet;
import delta.games.lotro.dat.data.enums.EnumMapper;
import delta.games.lotro.lore.geo.LandmarkDescription;
import delta.games.lotro.lore.items.Item;
import delta.games.lotro.lore.items.ItemsManager;
import delta.games.lotro.lore.mobs.MobReference;
import delta.games.lotro.lore.npc.NpcDescription;
import delta.games.lotro.lore.quests.Achievable;
import delta.games.lotro.lore.quests.objectives.ConditionType;
import delta.games.lotro.lore.quests.objectives.DefaultObjectiveCondition;
import delta.games.lotro.lore.quests.objectives.FactionLevelCondition;
import delta.games.lotro.lore.quests.objectives.InventoryItemCondition;
import delta.games.lotro.lore.quests.objectives.LandmarkDetectionCondition;
import delta.games.lotro.lore.quests.objectives.MonsterDiedCondition;
import delta.games.lotro.lore.quests.objectives.NpcTalkCondition;
import delta.games.lotro.lore.quests.objectives.SkillUsedCondition;
import delta.games.lotro.lore.quests.objectives.MonsterDiedCondition.MobSelection;
import delta.games.lotro.lore.quests.objectives.Objective;
import delta.games.lotro.lore.quests.objectives.ObjectiveCondition;
import delta.games.lotro.lore.quests.objectives.ObjectivesManager;
import delta.games.lotro.lore.quests.objectives.QuestCompleteCondition;
import delta.games.lotro.lore.reputation.Faction;
import delta.games.lotro.lore.reputation.FactionsRegistry;
import delta.games.lotro.tools.dat.characters.SkillLoader;
import delta.games.lotro.tools.dat.utils.DatUtils;
import delta.games.lotro.tools.dat.utils.MobLoader;
import delta.games.lotro.tools.dat.utils.NpcLoader;
import delta.games.lotro.tools.dat.utils.PlaceLoader;
import delta.games.lotro.utils.Proxy;

/**
 * Loader for quest/deed objectives from DAT files.
 * @author DAM
 */
public class DatObjectivesLoader
{
  private static final Logger LOGGER=Logger.getLogger(DatObjectivesLoader.class);

  private DataFacade _facade;

  private EnumMapper _monsterDivision;
  private EnumMapper _questEvent;
  private EnumMapper _questCategory;
  //private EnumMapper _deedCategory;

  private MobLoader _mobLoader;

  //public static HashSet<String> propNames=new HashSet<String>();

  /**
   * Constructor.
   * @param facade Data facade.
   */
  public DatObjectivesLoader(DataFacade facade)
  {
    _facade=facade;
    _monsterDivision=_facade.getEnumsManager().getEnumMapper(587202657);
    _questEvent=_facade.getEnumsManager().getEnumMapper(587202639);
    _questCategory=_facade.getEnumsManager().getEnumMapper(587202585);
    //_deedCategory=_facade.getEnumsManager().getEnumMapper(587202587);
    _mobLoader=new MobLoader(facade);
  }

  /**
   * Load quest/deed objectives from DAT files data.
   * @param objectivesManager Objectives manager.
   * @param properties Quest/deed properties.
   */
  public void handleObjectives(ObjectivesManager objectivesManager, PropertiesSet properties)
  {
    if (objectivesManager==null)
    {
      return;
    }
    Object[] objectivesArray=(Object[])properties.getProperty("Quest_ObjectiveArray");
    if (objectivesArray!=null)
    {
      // Can have several objectives (ordered)
      for(Object objectiveObj : objectivesArray)
      {
        PropertiesSet objectiveProps=(PropertiesSet)objectiveObj;

        Objective objective=new Objective();
        //System.out.println(objectiveProps.dump());
        // Index
        int objectiveIndex=((Integer)objectiveProps.getProperty("Quest_ObjectiveIndex")).intValue();
        //System.out.println("Objective #"+objectiveIndex);
        objective.setIndex(objectiveIndex);
        // Description
        String description=DatUtils.getFullStringProperty(objectiveProps,"Quest_ObjectiveDescription","{***}");
        //System.out.println("\tDescription: "+description);
        objective.setText(description);
        // Conditions (can have several conditions)
        Object[] completionConditionsArray=(Object[])objectiveProps.getProperty("Quest_CompletionConditionArray");
        if (completionConditionsArray!=null)
        {
          for(Object item : completionConditionsArray)
          {
            handleObjectiveItem(objective,item);
          }
        }
        objectivesManager.addObjective(objective);
      }
    }
    objectivesManager.sort();
  }

  private void handleObjectiveItem(Objective objective, Object item)
  {
    if (item instanceof Object[])
    {
      for(Object childItem : (Object[])item)
      {
        handleObjectiveItem(objective, childItem);
      }
    }
    else if (item instanceof PropertiesSet)
    {
      handleCompletionCondition(objective, (PropertiesSet)item);
    }
  }

  private void handleCompletionCondition(Objective objective, PropertiesSet properties)
  {
    /*
     * Shared condition attributes:
     * Accomplishment_LoreInfo: verbose text about the condition (deeds only)
     * QuestEvent_EventOrder: usually 0, but can be 1,2,3 if several conditions in an objective.
     * QuestEvent_ID: condition type identifier (see Enum: QuestEventType, (id=587202639))
     * QuestEvent_ProgressOverride: small text for the condition (ex: "Complete quests within the Shire")
     * QuestEvent_BillboardProgressOverride: optional, a String[]. Set if QuestEvent_ShowBillboardText=1?
     * QuestEvent_RoleConstraint: optional, key for additional constraint on condition (for instance, challenge mode for a quest)
     * QuestEvent_ShowBillboardText: usually 0, can be 1 (means that the condition shall be displayed in the UI?)
     * QuestEvent_ShowProgressText: optional, 0 if set, used many times
     */
    // Order
    Integer eventOrder=(Integer)properties.getProperty("QuestEvent_EventOrder");
    //System.out.println("\tEvent #"+eventOrder);
    // ID
    int questEventId=((Integer)properties.getProperty("QuestEvent_ID")).intValue();
    //System.out.println("\t\tEvent ID: "+questEventId+" ("+eventMeaning+")");
    // Billboard
    Integer showBillboardText=(Integer)properties.getProperty("QuestEvent_ShowBillboardText");
    if ((showBillboardText!=null) && (showBillboardText.intValue()!=0))
    {
      //System.out.println("\t\tShow billboard text: "+showBillboardText);
    }
    String billboardProgressOverride=DatUtils.getFullStringProperty(properties,"QuestEvent_BillboardProgressOverride",Markers.CHARACTER);
    if (billboardProgressOverride!=null)
    {
      //System.out.println("\t\tBillboard progress override: "+billboardProgressOverride);
    }
    // Progress override
    String progressOverride=DatUtils.getFullStringProperty(properties,"QuestEvent_ProgressOverride",Markers.CHARACTER);
    // Role constraint
    String roleConstraint=(String)properties.getProperty("QuestEvent_RoleConstraint");
    if (roleConstraint!=null)
    {
      //System.out.println("\t\tRole constraint: "+roleConstraint);
    }
    // Lore info
    String loreInfo=DatUtils.getStringProperty(properties,"Accomplishment_LoreInfo");

    // Deeds:
    // QuestEvent_ID: {32=3936(done), 22=1142(done), 1=889, 21=869(done), 26=611, 31=487(done), 45=411, 7=349, 25=116,
    // 34=108, 11=82, 39=51, 4=41, 18=29, 24=22, 16=20, 6=15, 10=2, 58=2, 59=1}
    // Quests:
    // QuestEvent_ID:  {11=11545, 7=3845, 22=3164(done), 1=2970, 32=1967(done), 34=1599, 10=1162, 31=831(done), 5=640, 21=425(done),
    // 24=301, 14=293, 19=210, 27=196, 13=187, 56=17320=144, 16=123, 37=97, 29=97, 33=72, 2=58, 59=50, 25=46,
    // 18=43, 26=39, 58=35, 57=35, 4=25, 46=16, 40=15, 39=14, 43=14, 30=12, 45=8, 38=5, 6=1, 9=1}

    /*
  1 => EnterDetection (deeds,quests)
  2 => LeaveDetection (quests)
  4 => MonsterPlayerDied (deeds,quests)
  5 => NPCUsed (quests)
  6 => SkillApplied (deeds,quests)
  7 => ItemUsed (deeds,quests)
  9 => Detecting (quests)
  10 => ExternalInventoryItem (deeds,quests)
  11 => NPCTalk (deeds,quests)
  13 => Channeling (quests)
  14 => TimeExpired (quests)
  16 => ItemTalk (deeds,quests)
  18 => Level (deeds,quests)
  19 => ClearCamp (quests)
  21 => LandmarkDetection (deeds,quests)
  22 => MonsterDied (deeds,quests)
  24 => Emote (deeds,quests)
  25 => PlayerDied (deeds,quests)
  26 => SkillUsed (deeds,quests)
  27 => KungFu (quests)
  29 => Escort (quests)
  30 => SelfDied (quests)
  31 => InventoryItem (deeds,quests)
  32 => QuestComplete (deeds,quests)
  33 => CraftRecipeExecution (quests)
  34 => WorldEventCondition (deeds,quests)
  37 => SessionFinished (quests)
  38 => ResourceSet (quests)
  39 => HobbyItem (deeds,quests)
  40 => ItemAdvancement (quests)
  43 => Dismounted (quests)
  45 => FactionLevel (deeds,quests)
  46 => Teleported (quests)
  56 => EnterPlayerAOI (quests)
  57 => CorpseUsed (quests)
  58 => ScriptCallback (deeds,quests)
  59 => QuestBestowed (deeds,quests)
 */

    ObjectiveCondition condition=null;
    ConditionType type=null;
    if (questEventId==1)
    {
      type=ConditionType.ENTER_DETECTION;
      handleEnterDetection(properties);
    }
    else if (questEventId==7)
    {
      type=ConditionType.ITEM_USED;
      handleItemUsed(properties);
    }
    else if (questEventId==11)
    {
      condition=handleNpcTalk(properties);
    }
    else if (questEventId==21)
    {
      condition=handleLandmarkDetection(properties);
    }
    else if (questEventId==22)
    {
      condition=handleMonsterDieCondition(properties);
    }
    else if (questEventId==24)
    {
      type=ConditionType.EMOTE;
      handleEmoteCondition(properties);
    }
    else if (questEventId==25)
    {
      type=ConditionType.PLAYER_DIED;
      handlePlayerDied(properties);
    }
    else if (questEventId==26)
    {
      condition=handleSkillUsed(properties);
    }
    else if (questEventId==31)
    {
      condition=handleInventoryItem(properties);
    }
    else if (questEventId==32)
    {
      condition=handleQuestComplete(properties);
    }
    else if (questEventId==34)
    {
      type=ConditionType.WORLD_EVENT_CONDITION;
      handleWorldEventCondition(properties);
    }
    else if (questEventId==39)
    {
      type=ConditionType.HOBBY_ITEM;
      handleHobbyItem(properties);
    }
    else if (questEventId==45)
    {
      condition=handleFactionLevel(properties);
    }
    else if (questEventId==2) type=ConditionType.LEAVE_DETECTION;
    else if (questEventId==4) type=ConditionType.MONSTER_PLAYER_DIED;
    else if (questEventId==5) type=ConditionType.NPC_USED;
    else if (questEventId==6) type=ConditionType.SKILL_APPLIED;
    else if (questEventId==9) type=ConditionType.DETECTING;
    else if (questEventId==10) type=ConditionType.EXTERNAL_INVENTORY_ITEM;
    else if (questEventId==13) type=ConditionType.CHANNELING;
    else if (questEventId==14) type=ConditionType.TIME_EXPIRED;
    else if (questEventId==16) type=ConditionType.ITEM_TALK;
    else if (questEventId==18) type=ConditionType.LEVEL;
    else if (questEventId==19) type=ConditionType.CLEAR_CAMP;
    else if (questEventId==27) type=ConditionType.KUNG_FU;
    else if (questEventId==29) type=ConditionType.ESCORT;
    else if (questEventId==30) type=ConditionType.SELF_DIED;
    else if (questEventId==33) type=ConditionType.CRAFT_RECIPE_EXECUTION;
    else if (questEventId==37) type=ConditionType.SESSION_FINISHED;
    else if (questEventId==38) type=ConditionType.RESOURCE_SET;
    else if (questEventId==40) type=ConditionType.ITEM_ADVANCEMENT;
    else if (questEventId==43) type=ConditionType.DISMOUNTED;
    else if (questEventId==46) type=ConditionType.TELEPORTED;
    else if (questEventId==56) type=ConditionType.ENTER_PLAYER_AOI;
    else if (questEventId==57) type=ConditionType.CORPSE_USED;
    else if (questEventId==58) type=ConditionType.SCRIPT_CALLBACK;
    else if (questEventId==59) type=ConditionType.QUEST_BESTOWED;
    else
    {
      String eventMeaning=_questEvent.getString(questEventId);
      LOGGER.warn("Unmanaged quest event: ID="+questEventId+", meaning="+eventMeaning);
    }

    if (condition==null)
    {
      condition=new DefaultObjectiveCondition(type);
    }
    if (eventOrder!=null)
    {
      condition.setIndex(eventOrder.intValue());
    }
    if (loreInfo!=null)
    {
      condition.setLoreInfo(loreInfo);
    }
    if (progressOverride!=null)
    {
      condition.setProgressOverride(progressOverride);
    }
    objective.addCondition(condition);
  }

  private void handleEnterDetection(PropertiesSet properties)
  {
    /*
     * QuestEvent_Detect: ID of NPC?, 20 times.
     * QuestEvent_HideRadarIcon: optional, 5 times, always 1.
     * QuestEvent_DramaProgressOverride: String[], 40 times, mainly for session play quests (chicken or horse)
     * QuestEvent_RoleConstraint seems to give the string ID of the thing to detect.
     * QuestEvent_ProgressOverride gives a useable text.
     */
    Integer detect=(Integer)properties.getProperty("QuestEvent_Detect");
    String roleConstraint=(String)properties.getProperty("QuestEvent_RoleConstraint");
    System.out.println("Enter detect: "+detect+", role="+roleConstraint);
  }

  private void handleItemUsed(PropertiesSet properties)
  {
    /*
     * QuestEvent_AllowQuickslot: optional, found 22 times Integer 0.
     * QuestEvent_RequireUniqueItems: optional, found 31 times Integer 1.
     * QuestEvent_Number: optional, number of usages, 1-300.
     * QuestEvent_HasInventoryItem: optional, found 2 times Integer 1.
     * QuestEvent_ItemDID: almost always (306/350). Item ID.
     * or QuestEvent_RoleConstraint: (65/350) for food, dwarves marker and hobbit lamps from Enedwaith...
     * (sometimes both, never none)
     * QuestEvent_DestroyInventoryItems: optional, found 16 times Integer 0 or 1.
     */
    //Integer itemId=(Integer)properties.getProperty("QuestEvent_ItemDID");
    //String constraint=(String)properties.getProperty("QuestEvent_RoleConstraint");
  }

  private NpcTalkCondition handleNpcTalk(PropertiesSet properties)
  {
    /*
     * QuestEvent_RoleConstraint: used 6 times for limlight spirits.
     * QuestEvent_NPCTalk: almost always (76/82). NPC ID?
     * Quest_Role: gives some text, sound ID... for the NPC... (50/82)
     * QuestEvent_CanTeleportToObjective: optional, used 12 times: Integer 0.
     * QuestEvent_Number: always 1.
[QuestEvent_WaitForDrama, QuestEvent_DramaName, Quest_GiveItemArray, QuestEvent_ShowProgressText, QuestEvent_RoleConstraint,
QuestEvent_ShowBillboardText, QuestEvent_ID, QuestEvent_ForceCheckContentLayer, QuestEvent_DramaProgressOverride,
QuestEvent_ProgressOverride, QuestEvent_GiveItemsOnAdvance, QuestEvent_HideRadarIcon, Accomplishment_LoreInfo,
QuestEvent_DisableEntityExamination, QuestEvent_BillboardProgressOverride, QuestEvent_IsRemote, QuestEvent_ItemDID, QuestEvent_RunDramaOnAdvance, QuestEvent_Locations_ForceFullLandblock, QuestEvent_LocationsAreOverrides, QuestEvent_Locations, QuestEvent_IsFellowUseShared, QuestEvent_ApplyEffectArray, Quest_Role]
     */

    NpcTalkCondition ret=new NpcTalkCondition();
    Integer npcId=(Integer)properties.getProperty("QuestEvent_NPCTalk");
    if (npcId!=null)
    {
      String npcName=NpcLoader.loadNPC(_facade,npcId.intValue());
      Proxy<NpcDescription> proxy=new Proxy<NpcDescription>();
      proxy.setId(npcId.intValue());
      proxy.setName(npcName);
      ret.setProxy(proxy);
    }
    return ret;
  }

  private LandmarkDetectionCondition handleLandmarkDetection(PropertiesSet properties)
  {
    LandmarkDetectionCondition ret=new LandmarkDetectionCondition();
    /*
    QuestEvent_ForceCheckContentLayer: optional, used 2 times: Integer 1
    QuestEvent_QuestComplete_SuppressQuestCountUpdate: optional, used once: Integer 1
    QuestEvent_LocationsAreOverrides: optional, used 20 times, always Integer 1
    Quest_Role: found 7 times, always empty.
    QuestEvent_LandmarkDID: POI identifier. always present (869 times).
    */
    Integer landmarkId=(Integer)properties.getProperty("QuestEvent_LandmarkDID");
    if (landmarkId!=null)
    {
      String landmarkName=PlaceLoader.loadLandmark(_facade,landmarkId.intValue());
      Proxy<LandmarkDescription> landmark=new Proxy<LandmarkDescription>();
      landmark.setId(landmarkId.intValue());
      landmark.setName(landmarkName);
      ret.setLandmarkProxy(landmark);
    }
    return ret;
  }

  private MonsterDiedCondition handleMonsterDieCondition(PropertiesSet properties)
  {
    /*
QuestEvent_MonsterGenus_Array: 
  #1: 
    Quest_MonsterRegion: 1879049792
    Quest_MonsterSpecies: 41
QuestEvent_Number: 1
QuestEvent_ShowBillboardText: 0
     */

    MonsterDiedCondition ret=new MonsterDiedCondition();

    Object[] monsterGenusArray=(Object[])properties.getProperty("QuestEvent_MonsterGenus_Array");
    if (monsterGenusArray!=null)
    {
      int nbMonsterGenus=monsterGenusArray.length;
      for(int i=0;i<nbMonsterGenus;i++)
      {
        PropertiesSet mobRegionProps=(PropertiesSet)monsterGenusArray[i];
        // Where
        Integer regionId=(Integer)mobRegionProps.getProperty("Quest_MonsterRegion");
        Integer mobDivision=(Integer)mobRegionProps.getProperty("Quest_MonsterDivision");
        Integer landmarkId=(Integer)mobRegionProps.getProperty("QuestEvent_LandmarkDID");
        String where=null;
        if (mobDivision!=null)
        {
          String divisionStr=_monsterDivision.getString(mobDivision.intValue());
          where=concat(where,divisionStr);
        }
        if (regionId!=null)
        {
          String regionName=PlaceLoader.loadPlace(_facade,regionId.intValue());
          where=concat(where,regionName);
        }
        if (landmarkId!=null)
        {
          String landmarkName=PlaceLoader.loadLandmark(_facade,landmarkId.intValue());
          where=concat(where,landmarkName);
        }
        // What
        MobReference mobReference=_mobLoader.buildMobReference(mobRegionProps);
        String what=(mobReference!=null)?mobReference.getLabel():null;
        MobSelection selection=new MobSelection();
        selection.setWhere(where);
        selection.setWhat(what);
        ret.getMobSelections().add(selection);
      }
    }
    else
    {
      Integer mobId=(Integer)properties.getProperty("QuestEvent_MonsterDID");
      if (mobId!=null)
      {
        String mobName=_mobLoader.loadMob(mobId.intValue());
        ret.setMobId(mobId);
        ret.setMobName(mobName);
      }
    }
    Integer nbTimes=(Integer)properties.getProperty("QuestEvent_Number");
    int count=(nbTimes!=null)?nbTimes.intValue():1;
    ret.setCount(count);
    return ret;
  }

  private void handleEmoteCondition(PropertiesSet properties)
  {
    int emoteId=((Integer)properties.getProperty("QuestEvent_EmoteDID")).intValue();
    Integer nbTimesInt=(Integer)properties.getProperty("QuestEvent_Number");
    Integer maxTimesPerDay=(Integer)properties.getProperty("QuestEvent_DailyMaximumIncrements");
    String loreInfo=DatUtils.getStringProperty(properties,"Accomplishment_LoreInfo");
    String progressOverride=DatUtils.getStringProperty(properties,"QuestEvent_ProgressOverride");
    String text="Perform emote "+emoteId;
    int nbTimes=(nbTimesInt!=null)?nbTimesInt.intValue():1;
    if (nbTimes>1) text=text+" "+nbTimes+" times";
    if (maxTimesPerDay!=null)
    {
      text=text+" (max "+maxTimesPerDay+" times/day)";
    }
    //System.out.println(text);
    if (loreInfo!=null)
    {
      //System.out.println(loreInfo);
    }
    if ((progressOverride!=null) && (!progressOverride.equals(loreInfo)))
    {
      //System.out.println(progressOverride);
    }
  }

  private void handlePlayerDied(PropertiesSet properties)
  {
    /*
     * QuestEvent_MonsterGenus_Array: always
     *    - Quest_MonsterClass: see Enum: CharacterClassType, (id=587202574). Ex: 162=Hunter
     *    - Quest_MonsterGenus (bitset for Enum: GenusType, (id=587202570)) and Quest_MonsterSpecies (see Enum: Species, (id=587202571)).
     *      Ex: 16384=2^(15-1) 15=Dwarf and 73 (Dwarves).
     * QuestEvent_TerritoryDID: Zone ID (Eregion, Forochel, Angmar)
     * QuestEvent_Number
     */
    /*
    Integer territoryId=(Integer)properties.getProperty("QuestEvent_TerritoryDID");
    if (territoryId!=null)
    {
      String name=PlaceLoader.loadPlace(_facade,territoryId.intValue());
      System.out.println("Territory: "+name);
    }
    */
  }

  private SkillUsedCondition handleSkillUsed(PropertiesSet properties)
  {
    /*
    // QuestEvent_Number: number of time to use the skill
    // QuestEvent_DailyMaximumIncrements: used often ~400/~600. Max number of skill usages counted by day
    //     QuestEvent_SkillDID: Skill ID (not mandatory)
    // or: QuestEvent_SkillQuestFlags: bitset: skill(s) indicator? Ex: 70368744177664=2^(47-1)
    // QuestEvent_Skill_AttackResultArray: always, array of Integer (see Enum: CombatResultType, (id=587202602))
    //   9: Hit, 10: CriticalHit, 11: SuperCriticalHit
    */

    SkillUsedCondition ret=new SkillUsedCondition();
    Integer skillId=(Integer)properties.getProperty("QuestEvent_SkillDID");
    if (skillId!=null)
    {
      SkillDescription skill=SkillLoader.getSkill(_facade,skillId.intValue());
      Proxy<SkillDescription> proxy=null;
      if (skill!=null)
      {
        proxy=new Proxy<SkillDescription>();
        proxy.setId(skill.getIdentifier());
        proxy.setName(skill.getName());
        proxy.setObject(skill);
        ret.setProxy(proxy);
      }
      else
      {
        LOGGER.warn("Skill not found: "+skillId);
      }
    }
    Integer countInt=(Integer)properties.getProperty("QuestEvent_Number");
    int count=(countInt!=null)?countInt.intValue():1;
    ret.setCount(count);
    Integer dailyMaxIncrement=(Integer)properties.getProperty("QuestEvent_DailyMaximumIncrements");
    if (dailyMaxIncrement!=null)
    {
      ret.setMaxPerDay(dailyMaxIncrement);
    }
    Long flags=(Long)properties.getProperty("QuestEvent_SkillQuestFlags");
    if ((flags!=null) && (flags.longValue()!=0))
    {
      System.out.println("Flags: "+flags);
    }
    //Object[] attackResultArray=(Object[])properties.getProperty("QuestEvent_Skill_AttackResultArray");
    //System.out.println(properties.dump());
    return ret;
  }

  private InventoryItemCondition handleInventoryItem(PropertiesSet properties)
  {
    /*
     * QuestEvent_ForceCheckContentLayer: optional, used once: Integer 1
     * QuestEvent_QuestComplete_SuppressQuestCountUpdate: optional, used once: Integer 1
     * QuestEvent_ItemDID: always, item ID
     * QuestEvent_Number: number of items to get.
     * QuestEvent_DestroyInventoryItems: optional, always 1 when present (398/488).
     *     Indicates if the item is destroyed when acquired or not.
     */
    InventoryItemCondition ret=new InventoryItemCondition();

    // Count
    Integer count=(Integer)properties.getProperty("QuestEvent_Number");
    if (count!=null)
    {
      ret.setCount(count.intValue());
    }
    Integer itemId=(Integer)properties.getProperty("QuestEvent_ItemDID");
    if (itemId!=null)
    {
      Item item=ItemsManager.getInstance().getItem(itemId.intValue());
      if (item!=null)
      {
        String itemName=item.getName();
        Proxy<Item> itemProxy=new Proxy<Item>();
        itemProxy.setId(itemId.intValue());
        itemProxy.setName(itemName);
        ret.setProxy(itemProxy);
      }
      else
      {
        LOGGER.warn("Could not find item with ID="+itemId);
      }
    }
    return ret;
  }

  private QuestCompleteCondition handleQuestComplete(PropertiesSet properties)
  {
    /*
    QuestEvent_QuestComplete_SuppressQuestCountUpdate, QuestEvent_Locations_ForceFullLandblock,
    // QuestEvent_QuestComplete: ID of quest/deed to complete
    // or QuestEvent_QuestCompleteCategory: category of quests to complete (see Enum: QuestCategory, (id=587202585)). Ex: 112=Task
    // or QuestEvent_AccomplishmentCompleteCategory: same for deed (see Enum: AccomplishmentCategory, (id=587202587))
    // QuestEvent_Number: number of quests to complete (1, 75...)
    // QuestEvent_DailyMaximumIncrements: used once, with value -1 => ignore!
    // QuestEvent_DisableEntityExamination: optional, 1 if set
    // QuestEvent_GiveItemsOnAdvance: optional, 1 if set, only used 2 times
    // QuestEvent_LocationsAreOverrides: optional, 1 if set, only used 5 times
    // QuestEvent_QuestCompleteScope: optional; 1 or 3 if set, used 8 times (see Enum: UIQuestScope, (id=587202590)?). 1=Repeatable ; 3=Small Fellowship
    */

    QuestCompleteCondition ret=new QuestCompleteCondition();

    // Count
    Integer count=(Integer)properties.getProperty("QuestEvent_Number");
    if (count!=null)
    {
      //System.out.println("\t\tQuests count: "+count);
      ret.setCompletionCount(count.intValue());
    }
    Integer questId=(Integer)properties.getProperty("QuestEvent_QuestComplete");
    if (questId!=null)
    {
      // Check if deed or quest
      Boolean isQuest=DatQuestDeedsUtils.isQuestId(_facade,questId.intValue());
      if (isQuest!=null)
      {
        Proxy<Achievable> proxy=new Proxy<Achievable>();
        proxy.setId(questId.intValue());
        ret.setProxy(proxy);
      }
    }
    else
    {
      // Quests in category
      Integer questCategoryCode=(Integer)properties.getProperty("QuestEvent_QuestCompleteCategory");
      if (questCategoryCode!=null)
      {
        String questCategory=_questCategory.getString(questCategoryCode.intValue());
        //System.out.println("\t\tQuest category: "+questCategoryCode+" => "+questCategory);
        ret.setQuestCategory(questCategory);
      }
      // Unused
      /*
      Integer deedCategoryCode=(Integer)properties.getProperty("QuestEvent_AccomplishmentCompleteCategory");
      if (deedCategoryCode!=null)
      {
        String deedCategory=_deedCategory.getString(deedCategoryCode.intValue());
        System.out.println("\t\tDeed category: "+deedCategoryCode+" => "+deedCategory);
        ret.setDeedCategory(deedCategory);
      }
      */
    }
    // Unused
    /*
    Integer dailyMax=(Integer)properties.getProperty("QuestEvent_DailyMaximumIncrements");
    if (dailyMax!=null)
    {
      System.out.println("dailyMax: "+dailyMax); // -1?
    }
    */
    return ret;
  }

  private void handleWorldEventCondition(PropertiesSet properties)
  {
    /*
     * QuestEvent_WorldEvent_Value: always. Integer 1, rarely 4 or 7.
     * QuestEvent_RoleConstraint: string. 4 uses: 2 for draigoch, 2 for skirmishes (goblin's pot in 21st hall, arrows in defence of bruinen)
     * QuestEvent_WorldEvent: ID (relates to world event, such as festivals)
     * QuestEvent_WorldEvent_Operator: always. Integer 3.
     * QuestEvent_Number: almost always. Integer 1 if set.
     */
  }

  private void handleHobbyItem(PropertiesSet properties)
  {
    //propNames.addAll(properties.getPropertyNames());
    //System.out.println("Condition properties: "+properties.dump());
    /*
     * QuestEvent_HobbyDID: always. 51 times the same hobby ID (the only one: 1879109150).
     * QuestEvent_ItemDID: always. Item ID (probably all fishes).
     * QuestEvent_Number: set 40 times. Mostly Integer: 1, sometimes 10.
     */
  }

  private FactionLevelCondition handleFactionLevel(PropertiesSet properties)
  {
    /*
     * Condition validated when the given faction tier is reached.
     * QuestEvent_FactionDID: always. Faction ID.
     * QuestEvent_ReputationTier: always. Faction tier: 1->7
     */

    int factionId=((Integer)properties.getProperty("QuestEvent_FactionDID")).intValue();
    int tier=((Integer)properties.getProperty("QuestEvent_ReputationTier")).intValue();
    FactionLevelCondition ret=new FactionLevelCondition();
    Proxy<Faction> factionProxy=new Proxy<Faction>();
    factionProxy.setId(factionId);
    Faction faction=FactionsRegistry.getInstance().getById(factionId);
    String factionName=(faction!=null)?faction.getName():"?";
    factionProxy.setName(factionName);
    ret.setProxy(factionProxy);
    ret.setTier(tier);
    return ret;
  }

  private String concat(String base, String add)
  {
    if (base==null) return add;
    return base+"/"+add;
  }
}