/*
 * This file is part of MOMO. A plugin providing more gamemodes for Mindustry servers.
 *
 * MIT License
 *
 * Copyright (c) 2024 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xpdustry.momo.tower;

import com.xpdustry.distributor.api.Distributor;
import com.xpdustry.distributor.api.annotation.EventHandler;
import com.xpdustry.distributor.api.annotation.PlayerActionHandler;
import com.xpdustry.distributor.api.plugin.PluginListener;
import com.xpdustry.momo.MoGameMode;
import com.xpdustry.momo.MoMoPlugin;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.net.Administration;
import mindustry.type.ItemSeq;
import mindustry.world.blocks.storage.CoreBlock;

public final class TowerLogic implements PluginListener {

    private final MoMoPlugin plugin;

    public TowerLogic(final MoMoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginInit() {
        final var pathfinder = new TowerPathfinder();
        Vars.pathfinder = pathfinder;
        this.plugin.addListener(pathfinder);
        this.plugin.addListener(new TowerRenderer());
    }

    @PlayerActionHandler
    boolean onCoreBuildInteract(final Administration.PlayerAction action) {
        if (!this.plugin.isActive(MoGameMode.TOWER_DEFENSE)) return true;
        return !((action.type == Administration.ActionType.depositItem
                        || action.type == Administration.ActionType.withdrawItem)
                && action.tile.block() instanceof CoreBlock);
    }

    @EventHandler
    void onUnitSpawn(final EventType.UnitSpawnEvent event) {
        if (this.plugin.isActive(MoGameMode.TOWER_DEFENSE) && event.unit.team() == Vars.state.rules.waveTeam) {
            event.unit.controller(new GroundTowerAI());
        }
    }

    @EventHandler
    void onUnitDeath(final EventType.UnitDestroyEvent event) {
        if (event.unit.team() != Vars.state.rules.waveTeam) return;
        final var items = new ItemSeq();
        final var data = this.plugin.getMoConfig().tower().units().get(event.unit.type());
        if (data == null) return;
        for (final var drop : data.drops()) drop.apply(items);
        Vars.state.rules.defaultTeam.core().items().add(items);
        Distributor.get().getEventBus().post(new TowerDropEvent(event.unit.x(), event.unit.y(), items));
    }
}
