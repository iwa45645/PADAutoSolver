package com.example.padautosolver;
import java.util.*;
/** Candidate generation contract: real inventory, verified requirements and helper evidence are mandatory. */
public interface TeamOptimizer {
 final class Member {public final String instance;public final boolean owned,identityConfirmed,detailsVerified,helperAvailable;public final Set<String> roles;public Member(String id,boolean own,boolean identity,boolean details,boolean helper,Set<String> roles){instance=id;owned=own;identityConfirmed=identity;detailsVerified=details;helperAvailable=helper;this.roles=roles;}}
 final class Result {public final List<List<Member>> candidates=new ArrayList<>();public final List<String> missing=new ArrayList<>();}
 Result propose(List<Member> inventory,Set<String> verifiedDungeonRequirements);
 static List<String> validate(List<Member> team,Set<String> required){List<String> issues=new ArrayList<>();if(team.size()!=6)issues.add("six slots required");Set<String> ids=new HashSet<>(),roles=new HashSet<>();for(int i=0;i<team.size();i++){Member m=team.get(i);if(!ids.add(m.instance))issues.add("duplicate instance");if(i<5&&!m.owned)issues.add("not owned");if(i==5&&!m.helperAvailable)issues.add("helper unverified");if(!m.identityConfirmed||!m.detailsVerified)issues.add("unverified member");roles.addAll(m.roles);}for(String role:required)if(!roles.contains(role))issues.add("missing "+role);return issues;}
}
