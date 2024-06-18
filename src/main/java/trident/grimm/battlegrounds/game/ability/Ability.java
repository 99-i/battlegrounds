package trident.grimm.battlegrounds.game.ability;

//represents an ability.
public class Ability {
	// F_KEY: ability is used when using hotkey to switch item from offhand to main
	// hand
	// (default is the f key)
	// item: ability is used when right clicking an item. the ability id is stored
	// in "ability-id" nbt tag.
	// double_jump: used exclusively by acrobat and called when the player double
	// jumps (implementation details in AbilityManager.)
	public enum AbilityType {
		F_KEY,
		ITEM,
		DOUBLE_JUMP
	}

	// the type of the ability.
	private AbilityType abilityType;

	// stored in "ability-id" nbt tag for ITEM abilities.
	private String abilityId;

	public Ability(AbilityType abilityType) {
		this.abilityType = abilityType;
	}

	public Ability(AbilityType abilityType, String abilityId) {
		this.abilityType = abilityType;
		this.abilityId = abilityId;
	}

	public AbilityType getAbilityType() {
		return this.abilityType;
	}

	public String getAbilityId() {
		return this.abilityId;
	}
}