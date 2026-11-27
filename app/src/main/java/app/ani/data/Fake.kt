package app.ani.data

import androidx.compose.ui.graphics.Color

data class Show(
    val title: String,
    val hue: Float,
    val year: Int,
    val eps: Int,
    val rating: String,
    val desc: String,
    val none: Boolean = false,
) {
    fun bg(): Color = Color.hsl(hue, 0.14f, 0.21f)
    fun fg(): Color = Color.hsl(hue, 0.14f, 0.48f)
}

/** Fake data from the prototype. Replaced by resolver data later. */
val FakeShows = listOf(
    Show("Frieren: Beyond Journey's End", 200f, 2023, 28, "8.9", "After the party that defeated the Demon King disbands, an elf mage who outlives her old companions sets out to understand the short human lives she barely noticed. A quiet journey about time, memory and the people we only appreciate once they are gone."),
    Show("Dungeon Meshi", 28f, 2024, 24, "8.7", "A party is wiped out by a dragon, and the survivors go back down to rescue their friend before she is digested. With no money for supplies, they decide to eat the monsters."),
    Show("Violet Evergarden", 260f, 2018, 13, "8.5", "A former soldier learns to write letters for others and slowly works out what the last words she heard actually meant."),
    Show("That Time I Got Reincarnated as a Slime Season 3", 150f, 2024, 24, "8.1", "The slime ruler of Tempest continues to build his nation while larger powers take notice."),
    Show("Mushishi", 110f, 2005, 26, "8.7", "A wandering mushi master travels through rural Japan helping people affected by strange, primitive life forms."),
    Show("Cowboy Bebop", 330f, 1998, 26, "8.8", "Bounty hunters drift across the solar system, chasing paydays and running from their pasts.", none = true),
    Show("Monster", 20f, 2004, 74, "8.8", "A surgeon saves a boy's life, and years later learns what he has done."),
    Show("Haikyu!!", 30f, 2014, 25, "8.5", "A short boy with big ambitions joins a high school volleyball team with a fallen reputation."),
)

val FakeEpisodes = listOf(
    "The Journey's End", "It Didn't Have to Be Magic", "Killing Magic", "The Hero's Statue",
    "Aura the Guillotine", "A Pleasant Little Trip", "Phantoms of the Dead", "The Land Where Souls Rest",
    "Winter Quiet", "Dungeon Plans", "Another Spring", "The Third Examination",
)
