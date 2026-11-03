export interface TournamentSummary {
  id: string; name: string; season: string; location: string; createdAt: string;
  teamCount: number; totalMatches: number; playedMatches: number; fixtureGenerated: boolean;
}
export interface TournamentTeam { id: string; tournamentId: string; name: string; shortName: string; color: string; }
export interface TournamentMatch {
  id: string; round: number;
  homeId: string; homeName: string; homeShortName: string; homeColor: string;
  awayId: string; awayName: string; awayShortName: string; awayColor: string;
  homeGoals: number | null; awayGoals: number | null; kickoffAt: string | null; field: string | null; version: number;
}
export interface Standing {
  position: number; teamId: string; name: string; shortName: string; color: string;
  played: number; won: number; drawn: number; lost: number;
  goalsFor: number; goalsAgainst: number; goalDifference: number; points: number; form: string[];
}
export interface TournamentDetail {
  tournament: TournamentSummary; teams: TournamentTeam[]; matches: TournamentMatch[]; standings: Standing[];
}
