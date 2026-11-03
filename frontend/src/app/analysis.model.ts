export interface Member { name: string; age: number; team: string; maritalStatus: string; education: string; }
export interface Team { team: string; members: number; averageAge: number; youngestAge: number; oldestAge: number; }
export interface Report {
  id: string; fileName: string; createdAt: string; demo: boolean;
  totalMembers: number; teamCount: number; averageAge: number; youngestAge: number; oldestAge: number;
  racing: { total: number; averageAge: number | null };
  marriedUniversity: Member[]; marriedUniversityTotal: number;
  riverNames: { name: string; count: number }[];
  teams: Team[]; ages: { label: string; count: number }[]; education: { label: string; count: number }[];
}
export interface HistoryItem { id: string; fileName: string; createdAt: string; totalMembers: number; }
