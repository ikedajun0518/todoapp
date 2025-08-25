<template>
    <div>
        <div class="top-container">
            <h1>目標一覧</h1>
            <div class="toolbar">
                <router-link class="btn btn-add-goal" :to="{ name: 'GoalCreate' }">新規目標</router-link>
            </div>
        </div>

        <table class="list list--clickable table--scroll">
            <tbody>
                <tr v-for="g in items" :key="g.id" class="list__row" @click="toDetail(g.id)">
                    <td class="list__cell--name">{{ g.name }}</td>
                    <td class="list__updatedAt">{{ new Date(g.updatedAt).toLocaleString() }}</td>
                </tr>
            </tbody>
        </table>

        <p class="err" v-if="error">{{ error }}</p>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue';
    import http from '@/api/http';

    interface Goal {
        id: number;
        name: string;
        description?: string;
        deletionProtected: boolean;
        createdAt: string;
        updatedAt: string; 
    }

    export default Vue.extend({
        data() {
            return {
                items: [] as Goal[],
                loading: false as boolean,
                error: null as string | null
            }
        },
        created() {
            this.load();
        },
        methods: {
            toDetail(id: number) {
                this.$router.push({ name: 'GoalDetail', params: { id: String(id) } });
            },
            async load() {
                this.loading = true;
                this.error = null;
                try {
                    const res = await http.get('/goals', { params: { page: 0, size: 1000 } });
                    this.items = (res.data && res.data.content) ? res.data.content : res.data;
                } catch (e: any) {
                    this.error = e?.pretty?.message || e.message;
                } finally {
                    this.loading = false;
                }
            },
        }
    })
</script>