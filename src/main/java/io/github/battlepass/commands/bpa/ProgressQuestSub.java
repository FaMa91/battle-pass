package io.github.battlepass.commands.bpa;

import io.github.battlepass.BattlePlugin;
import io.github.battlepass.cache.QuestCache;
import io.github.battlepass.commands.BpSubCommand;
import io.github.battlepass.controller.QuestController;
import io.github.battlepass.enums.Category;
import io.github.battlepass.objects.quests.Quest;
import io.github.battlepass.objects.user.User;
import io.github.battlepass.quests.workers.pipeline.steps.QuestValidationStep;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProgressQuestSub extends BpSubCommand<CommandSender> {
    private final QuestCache questCache;
    private final QuestController controller;
    private final QuestValidationStep questValidationStep;

    public ProgressQuestSub(BattlePlugin plugin) {
        super(plugin);
        this.questCache = plugin.getQuestCache();
        this.controller = plugin.getQuestController();
        this.questValidationStep = new QuestValidationStep(plugin);

        this.inheritPermission();
        this.addFlats("progress", "quest");
        this.addArgument(User.class, "player", sender -> Bukkit.getOnlinePlayers()
                .stream()
                .map(Player::getName)
                .collect(Collectors.toList()));
        this.addArgument(Integer.class, "week");
        this.addArgument(String.class, "quest id");
        this.addArgument(BigInteger.class, "amount");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Optional<User> maybeUser = this.parseArgument(args, 2);
        Player player = maybeUser.map(value -> Bukkit.getPlayer(value.getUuid())).orElse(null);
        int week = this.parseArgument(args, 3);
        String id = this.parseArgument(args, 4);
        BigInteger amount = this.parseArgument(args, 5);

        if (!maybeUser.isPresent() || player == null) {
            this.lang.external("could-not-find-user", replacer -> replacer.set("player", args[2])).to(sender);
            return;
        }
        if (player.hasPermission("battlepass.block") && this.plugin.getConfig("settings").bool("enable-ban-permission") && !sender.isOp()) {
            this.lang.local("blocked-from-pass", sender.getName()).to(sender);
            return;
        }
        User user = maybeUser.get();
        Quest quest = this.questCache.getQuest(Category.WEEKLY.id(week), id);
        if (quest == null) {
            this.lang.local("invalid-quest-id", args[4], args[3].toLowerCase()).to(sender);
            return;
        }
        if (this.controller.isQuestDone(user, quest)) {
            this.lang.local("quest-already-done", args[2]);
            return;
        }
        if (this.questValidationStep.proceed(player, user, quest, amount, null, false)) {
            this.lang.local("successful-quest-progress", quest.getName()).to(sender);
        } else {
            this.lang.local("failed-quest-progress", quest.getName()).to(sender);
        }
    }
}