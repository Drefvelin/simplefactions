package me.Plugins.SimpleFactions.government.session;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.SimpleFactions.government.proposal.Proposal;

public class SessionReport {
    private static class ProposalResult {
        Proposal proposal;
        VoteResult result;
        int yay, nay, abstain;
        
        ProposalResult(Proposal proposal, VoteResult result, int yay, int nay, int abstain) {
            this.proposal = proposal;
            this.result = result;
            this.yay = yay;
            this.nay = nay;
            this.abstain = abstain;
        }
    }
    
    private List<ProposalResult> results = new ArrayList<>();
    
    public void addResult(Proposal proposal, VoteResult result, int yay, int nay, int abstain) {
        results.add(new ProposalResult(proposal, result, yay, nay, abstain));
    }
    
    public ItemStack generateReportBook() {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        
        meta.setTitle("§bSession Report");
        meta.setAuthor("Government");
        
        List<String> pages = new ArrayList<>();
        StringBuilder currentPage = new StringBuilder();
        
        // Title page
        currentPage.append(StringFormatter.formatHex("#93c9a7Session Report\n\n"));
        currentPage.append(StringFormatter.formatHex("#85c265Total Proposals\n"));
        currentPage.append(StringFormatter.formatHex("#c2bea7" + results.size() + "\n\n"));
        
        int passed = 0, failed = 0, tied = 0;
        for (ProposalResult pr : results) {
            if (pr.result == VoteResult.PASSED) passed++;
            else if (pr.result == VoteResult.FAILED) failed++;
            else if (pr.result == VoteResult.TIE) tied++;
        }
        
        currentPage.append(StringFormatter.formatHex("#45afc4Passed: #c2bea7" + passed + "\n"));
        currentPage.append(StringFormatter.formatHex("#89504eFailed: #c2bea7" + failed + "\n"));
        currentPage.append(StringFormatter.formatHex("#e3d5a1Tied: #c2bea7" + tied));
        
        pages.add(currentPage.toString());
        
        // Proposal detail pages
        int proposalNum = 1;
        for (ProposalResult pr : results) {
            currentPage = new StringBuilder();
            
            String resultColor = pr.result == VoteResult.PASSED ? "#45afc4" : 
                                pr.result == VoteResult.FAILED ? "#89504e" : "#e3d5a1";
            String resultText = pr.result == VoteResult.PASSED ? "PASSED" : 
                               pr.result == VoteResult.FAILED ? "FAILED" : "TIED";
            
            currentPage.append(StringFormatter.formatHex("#93c9a7Proposal " + proposalNum + "\n\n"));
            currentPage.append(StringFormatter.formatHex(resultColor + "§l" + resultText + "\n\n"));
            
            currentPage.append(StringFormatter.formatHex("#b8ae61Type: #c2bea7"));
            currentPage.append(pr.proposal.isLawProposal() ? "Law" : "Tax");
            currentPage.append("\n\n");
            
            currentPage.append(StringFormatter.formatHex("#b8ae61Votes:\n"));
            currentPage.append(StringFormatter.formatHex("#45afc4Yay: #c2bea7" + pr.yay + "\n"));
            currentPage.append(StringFormatter.formatHex("#89504eNay: #c2bea7" + pr.nay + "\n"));
            currentPage.append(StringFormatter.formatHex("#e3d5a1Abstain: #c2bea7" + pr.abstain));
            
            pages.add(currentPage.toString());
            proposalNum++;
        }
        
        meta.setPages(pages);
        item.setItemMeta(meta);
        return item;
    }
}
