/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pkg.data;

//import com.sun.org.apache.bcel.internal.generic.AALOAD;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import pkg.dataholder.WorldDataHolder;
import pkg.events.DPUserEvent.Action;
import pkg.main.DeliciousPermissions;

/**
 * 
 * @author gabrielcouto/ElgarL
 */
public class User extends DataUnit implements Cloneable {

	/**
     *
     */
	private String group = null;
	private final List<String> subGroups = Collections.synchronizedList(new ArrayList<String>());
	/**
	 * This one holds the fields in INFO node. like prefix = 'c' or build =
	 * false
	 */
	private UserVariables variables = new UserVariables(this);
	private transient Player bukkitPlayer = null;

	/**
	 * 
	 * @param name
	 */
	public User(WorldDataHolder source, String name) {

		super(source, name);
		this.group = source.getDefaultGroup().getName();
	}

	/**
	 * 
	 * @return User clone
	 */
	@Override
	public User clone() {

		User clone = new User(getDataSource(), this.getLastName());
		clone.group = this.group;
		
		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListStringCopy());
		
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		// clone.variables = this.variables.clone();
		// clone.flagAsChanged();
		return clone;
	}

	/**
	 * Use this to deliver a user from one WorldDataHolder to another
	 * 
	 * @param dataSource
	 * @return null if given dataSource already contains the same user
	 */
	public User clone(WorldDataHolder dataSource) {

		if (dataSource.isUserDeclared(this.getUUID())) {
			return null;
		}
		
		User clone = dataSource.createUser(this.getUUID());
		
		if (dataSource.getGroup(group) == null) {
			clone.setGroup(dataSource.getDefaultGroup());
		} else {
			clone.setGroup(dataSource.getGroup(this.getGroupName()));
		}
		
		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListStringCopy());
				
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		
		clone.variables = this.variables.clone(this);
		clone.flagAsChanged();
		return clone;
	}
	
	public User clone(String uUID, String CurrentName) {

		User clone = this.getDataSource().createUser(uUID);
		
		clone.setLastName(CurrentName);
		
		// Set the group silently.
		clone.setGroup(this.getDataSource().getGroup(this.getGroupName()), false);
		
		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListStringCopy());
		
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		
		clone.variables = this.variables.clone(this);
		clone.flagAsChanged();
		
		return clone;
	}

	public Group getGroup() {

		Group result = getDataSource().getGroup(group);
		if (result == null) {
			this.setGroup(getDataSource().getDefaultGroup());
			result = getDataSource().getDefaultGroup();
		}
		return result;
	}

	/**
	 * @return the group
	 */
	public String getGroupName() {

		Group result = getDataSource().getGroup(group);
		if (result == null) {
			group = getDataSource().getDefaultGroup().getName();
		}
		return group;
	}
	
	/**
	 * Place holder to let people know to stop using this method.
	 * 
	 * @deprecated use {@link #getLastName()} and {@link #getUUID()}.
	 * @return a string containing the players last known name.
	 */
	@Deprecated
	public String getName() {
		
		return this.getLastName();
		
	}


	/**
	 * @param group
	 *            the group to set
	 */
	public void setGroup(Group group) {

		setGroup(group, true);
	}

	/**
	 * @param group the group to set
	 * @param updatePerms if we are to trigger a superperms update.
	 * 
	 */
	public void setGroup(Group group, Boolean updatePerms) {

		if (!this.getDataSource().groupExists(group.getName())) {
			getDataSource().addGroup(group);
		}
		group = getDataSource().getGroup(group.getName());
		String oldGroup = this.group;
		this.group = group.getName();
		flagAsChanged();
		if (DeliciousPermissions.isLoaded()) {
			if (!DeliciousPermissions.BukkitPermissions.isPlayer_join() && (updatePerms))
				DeliciousPermissions.BukkitPermissions.updatePlayer(getBukkitPlayer());

			// Do we notify of the group change?
			String defaultGroupName = getDataSource().getDefaultGroup().getName();
			// if we were not in the default group
			// or we were in the default group and the move is to a different
			// group.
			boolean notify = (!oldGroup.equalsIgnoreCase(defaultGroupName)) || ((oldGroup.equalsIgnoreCase(defaultGroupName)) && (!this.group.equalsIgnoreCase(defaultGroupName)));

			if (notify)
				DeliciousPermissions.notify(this.getLastName(), String.format(" moved to the group %s in %s.", group.getName(), this.getDataSource().getName()));

			if (updatePerms)
				DeliciousPermissions.getGMEventHandler().callEvent(this, Action.USER_GROUP_CHANGED);
		}
	}

	public boolean addSubGroup(Group subGroup) {

		// Don't allow adding a subgroup if it's already set as the primary.
		if (this.group.equalsIgnoreCase(subGroup.getName())) {
			return false;
		}
		// User already has this subgroup
		if (containsSubGroup(subGroup))
			return false;

		// If the group doesn't exists add it
		if (!this.getDataSource().groupExists(subGroup.getName())) {
			getDataSource().addGroup(subGroup);
		}

		subGroups.add(subGroup.getName());
		flagAsChanged();
		if (DeliciousPermissions.isLoaded()) {
			if (!DeliciousPermissions.BukkitPermissions.isPlayer_join())
				DeliciousPermissions.BukkitPermissions.updatePlayer(getBukkitPlayer());
			DeliciousPermissions.getGMEventHandler().callEvent(this, Action.USER_SUBGROUP_CHANGED);
		}
		return true;

		//subGroup = getDataSource().getGroup(subGroup.getName());
		//removeSubGroup(subGroup);
		//subGroups.add(subGroup.getName());
	}

	public int subGroupsSize() {

		return subGroups.size();
	}

	public boolean isSubGroupsEmpty() {

		return subGroups.isEmpty();
	}

	public boolean containsSubGroup(Group subGroup) {

		return subGroups.contains(subGroup.getName());
	}

	public boolean removeSubGroup(Group subGroup) {

		try {
			if (subGroups.remove(subGroup.getName())) {
				flagAsChanged();
				if (DeliciousPermissions.isLoaded())
					if (!DeliciousPermissions.BukkitPermissions.isPlayer_join())
						DeliciousPermissions.BukkitPermissions.updatePlayer(getBukkitPlayer());
				DeliciousPermissions.getGMEventHandler().callEvent(this, Action.USER_SUBGROUP_CHANGED);
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	public ArrayList<Group> subGroupListCopy() {

		ArrayList<Group> val = new ArrayList<Group>();
		synchronized(subGroups) {
		for (String gstr : subGroups) {
			Group g = getDataSource().getGroup(gstr);
			if (g == null) {
				removeSubGroup(g);
				continue;
			}
			val.add(g);
		}
		}
		return val;
	}

	public ArrayList<String> subGroupListStringCopy() {
		synchronized(subGroups) {
			return new ArrayList<String>(subGroups);
		}
	}

	/**
	 * @return the variables
	 */
	public UserVariables getVariables() {

		return variables;
	}

	/**
	 * 
	 * @param varList
	 */
	public void setVariables(Map<String, Object> varList) {

		//UserVariables temp = new UserVariables(this, varList);
		variables.clearVars();
		for (String key : varList.keySet()) {
			variables.addVar(key, varList.get(key));
		}
		flagAsChanged();
		if (DeliciousPermissions.isLoaded()) {
			//if (!GroupManager.BukkitPermissions.isPlayer_join())
			//	GroupManager.BukkitPermissions.updatePlayer(this.getName());
			DeliciousPermissions.getGMEventHandler().callEvent(this, Action.USER_INFO_CHANGED);
		}
	}

	
	public User updatePlayer(Player player) {

		bukkitPlayer = player;
		return this;
	}

	@SuppressWarnings("deprecation")
	public Player getBukkitPlayer() {

		if (bukkitPlayer == null) {
			bukkitPlayer = Bukkit.getPlayer(this.getLastName());
		}
		return bukkitPlayer;
	}
}
